package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Passive object representing a single CPU.
 * Add all the fields described in the assignment as private fields.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class CPU {

    public enum CPUStatus {Processing, Waiting }
    private int cores;
    private Collection<DataBatch> data;
    private Cluster cluster;
    private int currTick;
    private DataBatch currDataBatch;
    private int ticksUntilDone;
    private CPUStatus status;

    public CPU(int cores, Cluster cluster){
        this.cores = cores;
        data = new LinkedList<>();
        this.cluster = cluster;
        currTick = 0;
        status= CPUStatus.Waiting;
    }


    /**
     *the CPUService will call this method when it receives a tickBroadcast from the messageBus.
     *@post: @currTick - @pre:currTick == 1
     */
    public void updateTick(){
        currTick = currTick + 1;
        if(status==CPUStatus.Processing ){
            cluster.setCPUTimeUnitsUsed();
            if(currTick== ticksUntilDone){
                doneProcessing();
            }
        }
        else{
            getDataBatch();
        }
    }

    /**
     * gets dataBatches from the cluster.
     * @post: data.size() > @pre:data.size()
     */
    public void getDataBatch(){
        DataBatch db= cluster.sendDataBatchToCPU();
        if(db!=null){
            data.add(db);
            startProcessing();
        }
    }

    /**
     * sets the current dataBatch the CPU is working on, and the number of ticks it takes to process it.
     * @pre: data.size() != 0
     * @post: currDataBatch != null
     */
    public void startProcessing(){
        if(!data.isEmpty() && status==CPUStatus.Waiting){
            currDataBatch= ((LinkedList<DataBatch>)data).peek();
            Data.Type type = currDataBatch.getDataType();
            if(type.equals(Data.Type.Images)){
                ticksUntilDone = (32/cores) * 4;
            }else if(type.equals(Data.Type.Text)){
                ticksUntilDone = (32/cores)*2;
            }else if(type.equals(Data.Type.Tabular)){
                ticksUntilDone = (32/cores);
            }
            currTick = 0;
            status= CPUStatus.Processing;
        }
    }

    /**
     * send the processed data batch to the cluster.
     * @pre: data.size > 0
     * @post: data.size == @pre: data.size - 1
     */
    public void sendDataBatch(){
        if(currDataBatch!=null){
            cluster.receiveDataBatchFromCPU(currDataBatch);
            data.remove(currDataBatch);
        }
    }

    /**
     * checks if the number of ticks it takes to process the data batch have passed,
     * if so starts the processing of another batch.
     * @post: if(ticksUntilDone == currTick) {data.size() == @pre:data.size() -1;}
     *
     */
    public void doneProcessing(){
        sendDataBatch();
        currDataBatch= null;
        status= CPUStatus.Waiting;
        getDataBatch();
    }

    public int getCurrTick() {
        return currTick;
    }

    public int getTicksUntilDone() {
        return ticksUntilDone;
    }

    public int getCores() {
        return cores;
    }

    public Collection<DataBatch> getData() {
        return data;
    }

    public DataBatch getCurrDataBatch() {
        return currDataBatch;
    }

    public void setCurrTick(int currTick) {
        this.currTick = currTick;
    }

    public void setStatus(CPUStatus status) {
        this.status = status;
    }
}