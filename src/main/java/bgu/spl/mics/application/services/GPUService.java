package bgu.spl.mics.application.services;

import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.GPU.GPUStatus;
import bgu.spl.mics.Event;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.objects.GPU;
import bgu.spl.mics.application.objects.Model;

import java.util.LinkedList;

/**
 * GPU service is responsible for handling the
 * {@link TrainModelEvent} and {@link TestModelEvent},
 * This class may not hold references for objects which it is not responsible for.
 *
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class GPUService extends MicroService {

    private boolean isBusy;
    private Model currModel;
    private GPU GPU;
    private TrainModelEvent currEvent;
    private LinkedList<Event<Model>> eventsInLine;
    private int total = 0;

    public GPUService(String name,GPU gpu) {
        super(name);
        GPU= gpu;
        isBusy=false;
        eventsInLine= new LinkedList<>();
    }

    @Override
    protected void initialize() {
        subscribeBroadcast(TerminationBroadcast.class, c -> terminate());

        subscribeBroadcast(TickBroadcast.class, c ->{
            GPU.updateTick();
            if(GPU.getStatus()==GPUStatus.Completed){
                GPU.setStatus(GPUStatus.Waiting);
                currModel= GPU.getModel();
                complete(currEvent, currModel);
                if(!eventsInLine.isEmpty()){
                    startProcessingEvent();
                }
                else{
                    isBusy=false;
                }
            }

        } );
        subscribeEvent(TrainModelEvent.class, c -> {
            if(!isBusy){
                isBusy=true;
                GPU.trainModel(c.getModel());
                currModel=c.getModel();
                currEvent= c;
            }
            else{
                // sort by size of data
                int i = 0;
                int size = c.getModel().getData().getSize();
                for(Event<Model> m : eventsInLine){
                    if(m.getClass() == TrainModelEvent.class){
                        if(((TrainModelEvent) m).getModel().getData().getSize() < size){
                            i++;
                        }
                    }
                }
                eventsInLine.add(i, c); //saves the event for when it finishes training this model*/
            }
        });

        subscribeEvent(TestModelEvent.class, c -> {
            if(!isBusy){
                isBusy=true;
                GPU.testModel(c.getModel());
                complete(c, c.getModel());
                isBusy=false;
            }
            else{
                eventsInLine.addFirst(c);
            }
        });

    }

    private void startProcessingEvent() {
        isBusy=true;
        Event<Model> e= eventsInLine.poll();
        if(e!=null && e.getClass()==TrainModelEvent.class){
            currEvent= (TrainModelEvent) e;
            currModel= currEvent.getModel();
            GPU.trainModel(currModel);
        }
        else if(e!=null && e.getClass()==TestModelEvent.class){
            currModel= ((TestModelEvent) e).getModel();
            GPU.testModel(currModel);
            complete(e, ((TestModelEvent) e).getModel());
        }
    }

}
