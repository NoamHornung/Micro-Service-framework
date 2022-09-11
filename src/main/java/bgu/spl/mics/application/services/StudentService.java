package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.Model;
import bgu.spl.mics.application.objects.Student;

import java.util.ArrayList;

/**
 * Student is responsible for sending the {@link TrainModelEvent},
 * {@link TestModelEvent} and {@link PublishResultsEvent}.
 * In addition, it must sign up for the conference publication broadcasts.
 * This class may not hold references for objects which it is not responsible for.
 *
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class StudentService extends MicroService {
    private Student student;
    private int index;
    private boolean inTrain;
    private boolean inTest;
    int numOfModels;
    ArrayList<Model> models;

    public StudentService(String name, Student student) {
        super(name);
        this.student= student;
        index=0;
        inTrain=false;
        inTest=false;
        models= student.getModels();
        numOfModels= models.size();
    }

    @Override
    protected void initialize() {

        subscribeBroadcast(TerminationBroadcast.class, c -> terminate());

        subscribeBroadcast(TickBroadcast.class,c -> {
            if(index< numOfModels){
                Model curr = models.get(index);
                Model.Status status= curr.getStatus();
                if(!inTrain && status.equals(Model.Status.PreTrained)){
                    sendEvent(new TrainModelEvent(curr));
                    inTrain=true;
                }
                else if(!inTest && status.equals(Model.Status.Trained)){
                    inTrain=false;
                    inTest=true;
                    sendEvent(new TestModelEvent(curr));
                }
                else if(status.equals(Model.Status.Tested)){
                    if(curr.getResult().equals(Model.Result.Good)){
                        sendEvent(new PublishResultsEvent(models.get(index)));
                    }
                    index++;
                    inTest=false;
                    inTrain=false;
                }
            }
        });


        subscribeBroadcast(PublishConferenceBroadcast.class, c -> {
            ArrayList<Model> publishModels= c.getModels();
            for(Model m: publishModels){
                if (m.getStudent().equals(student)){
                    student.increasePublications();
                }
                else{
                    student.increasePapersRead();
                }
            }

        });

    }
}
