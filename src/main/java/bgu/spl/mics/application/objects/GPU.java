package bgu.spl.mics.application.objects;

import bgu.spl.mics.application.services.GPUService;
import java.util.Collection;
import java.util.LinkedList;
import static bgu.spl.mics.application.objects.GPU.Type.*;

/**
 * Passive object representing a single GPU.
 * Add all the fields described in the assignment as private fields.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class GPU {


    /**
     * Enum representing the type of the GPU.
     */
    public enum Type {RTX3090, RTX2080, GTX1080}
    public enum GPUStatus {Training, Waiting, Completed}

    private Type type;
    private Model model;
    private  Cluster cluster;
    private int MaxNumOfProcessedBatches;
    private int ticksUntilDone; //number of ticks it takes to train the current batch
    private int currTick;
    private LinkedList<DataBatch> unprocessedDBs; //batches before processing
    private LinkedList<DataBatch> processedDBs; //batches after processing
    int numOfTrainedDBs; //number of data batches trained so far
    int numOfTotalDBs; //total number of data batches
    private DataBatch currDBInTraining; //current DB the GPU trains
    private GPUStatus status;


    public GPU(Type type,Cluster cluster){
        this.type = type;
        this.cluster = cluster;
        unprocessedDBs = new LinkedList<>();
        processedDBs = new LinkedList<>();
        if (this.type == RTX3090){
            MaxNumOfProcessedBatches = 32;
            ticksUntilDone=1;
        }else if(this.type == RTX2080){
            MaxNumOfProcessedBatches = 16;
            ticksUntilDone=2;
        }else if(this.type == GTX1080){
            MaxNumOfProcessedBatches = 8;
            ticksUntilDone=4;
        }
        currTick = 0;
        currDBInTraining = null;
        status= GPUStatus.Waiting;
    }

    public void reset(){
        unprocessedDBs.clear();
        processedDBs.clear();
        currTick=0;
        numOfTotalDBs=0;
        currDBInTraining= null;
        numOfTrainedDBs=0;
        status= GPUStatus.Waiting;
    }

    /**
     * tests the model.
     * @pre: model.getResult() == None
     * @post: model.getResult() == Good | Bad
     */
    public void testModel(Model model){
        this.model =model;
        double num= Math.random();
        Model.Result result;
        if(model.getStudent().getStatus()==Student.Degree.PhD){
            if(num<=0.8)
                result= Model.Result.Good;
            else
                result= Model.Result.Bad;
        }
        else{
            if(num<=0.6)
                result= Model.Result.Good;
            else
                result= Model.Result.Bad;
        }
        model.setResult(result);
        model.setStatus(Model.Status.Tested);
    }

    /**
     *the GPUService will call this method when it receives a tickBroadcast from the messageBus.
     *@post: @currTick - @pre:currTick == 1
     */
    public void updateTick(){
        currTick ++;
        if(status==GPUStatus.Training){
            cluster.setGPUTimeUnitsUsed();
            if(currTick==ticksUntilDone){
                doneTraining();
            }
        }
        else{
            receiveDataBatchFromCluster();
        }

    }

    /**
     * splits the data to dataBatches.
     * @post: numOfTotalDBs == model.getData().getSize()/1000
     */
    public void prepareDataBatches(){
        for(int i=0; i<model.getData().getSize();i=i+1000){
            unprocessedDBs.add(new DataBatch(model.getData(),i));
        }
        numOfTotalDBs = unprocessedDBs.size(); //numOfTotalDBs =model.getData().getSize()/1000
    }

    /**
     * sends unprocessed data batch to the cluster.
     * @pre: GPU.getUnprocessedDBs().size() > 0
     * @post: if(!unprocessedDBs.isEmpty() && processedDBs.size()<MaxNumOfProcessedBatches){
     *  GPU.getUnprocessedDBs().size() == @pre:GPU.getUnprocessedDBs().size() -1
     */
    public void sendDataBatchesToCluster(){
        if(!unprocessedDBs.isEmpty() && processedDBs.size()<MaxNumOfProcessedBatches){
            cluster.receiveDataBatchFromGPU(unprocessedDBs.poll(),this);
        }
    }

    /**
     * receives processed data batch from the cluster.
     * @inv: GPU.getProcessedDBs().size() <= MaxNumOfProcessedBatches
     * @post: GPU.getProcessedDBs().size() == @pre:GPU.getProcessedDBs().size() + 1
     */
    public void receiveDataBatchFromCluster(){
        if(processedDBs.size()<MaxNumOfProcessedBatches){
            DataBatch dataBatch= cluster.sendDataBatchToGPU(this);
            if(dataBatch!=null) {
                processedDBs.add(dataBatch);
                if (status == GPUStatus.Waiting) {
                    startTraining();
                }
            }
        }
    }
    /**
    * @post: model.getStatus() == Training
     */
    public void trainModel(Model model){
        reset();
        this.model= model;
        model.setStatus(Model.Status.Training);
        prepareDataBatches();
        for(int i=0; i<MaxNumOfProcessedBatches && !unprocessedDBs.isEmpty(); i++){
            sendDataBatchesToCluster();
        }
        receiveDataBatchFromCluster();
    }

    /**
     * starts to train a processed data batch.
     * @pre: processedDBs.size() > 0
     * @post: ticksUntilDone != 0
     * @post: currTick == 0
     * @post: GPUstatus= Training
     * @post: currDBInTraining != null
     *
     */
    public void startTraining(){
        if(!processedDBs.isEmpty()){
            currDBInTraining = processedDBs.peek();
            currTick= 0;
            status= GPUStatus.Training;
        }
    }

    /**
     * checks if the ticksUntilDone is equal to the currTick, if so starts the training of another batch.
     * @post: if(ticksUntilDone == currTick) {processedDBs.size() == @pre:processedDBs.size() -1;
     *                                        model.getData().getProcessed() == @pre:model.getData().getProcessed() + 1}
     */
    public void doneTraining(){
        model.getData().setProcessed(model.getData().getProcessed()+1000);
        numOfTrainedDBs++;
        processedDBs.poll();
        status= GPUStatus.Waiting;
        currDBInTraining=null;
        sendDataBatchesToCluster();
        if(numOfTrainedDBs==numOfTotalDBs){
            complete();
        }
        else{
            receiveDataBatchFromCluster();
        }
    }

    /**
     * notifies the GPUService that it finished training the model.
     * @pre: model.status == "PreTrained"
     * @pre: model.getData().getProcessed() == model.getData().getSize()
     * @pre: numOfTrainedDBs == numOfTotalDBs
     * @post: model.status == "Trained"
     */
    public void complete(){
        model.setStatus(Model.Status.Trained);
        cluster.setModelsNames(model.getName());
        status= GPUStatus.Completed;
    }

    public GPUStatus getStatus() {
        return status;
    }

    public void setStatus(GPUStatus status) {
        this.status = status;
    }

    public Model getModel() {
        return model;
    }

    public Type getType() {
        return type;
    }

    public int getMaxNumOfProcessedBatches() {
        return MaxNumOfProcessedBatches;
    }

    public int getTicksUntilDone() {
        return ticksUntilDone;
    }

    public Collection<DataBatch> getUnprocessedDBs() {
        return unprocessedDBs;
    }

    public Collection<DataBatch> getProcessedDBs() {
        return processedDBs;
    }

    public int getNumOfTrainedDBs() {
        return numOfTrainedDBs;
    }

    public int getNumOfTotalDBs() {
        return numOfTotalDBs;
    }

    public int getCurrTick() {
        return currTick;
    }

    public DataBatch getCurrDBInTraining() {
        return currDBInTraining;
    }

    public void setCurrTick(int currTick) {
        this.currTick = currTick;
    }

    public void setCurrDBInTraining(DataBatch currDBInTraining) {
        this.currDBInTraining = currDBInTraining;
    }

    public void setModel(Model model) {
        this.model = model;
    }
}