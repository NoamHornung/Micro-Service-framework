package bgu.spl.mics.application.objects;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static bgu.spl.mics.application.objects.GPU.Type.RTX2080;
import static org.junit.Assert.*;

public class GPUTest {
    GPU GPU;
    Model model;
    Cluster cluster;
    @Before
    public void setUp(){
        cluster= Cluster.getInstance();
        cluster.clear();
        GPU = new GPU(RTX2080,Cluster.getInstance());
        CPU cpu= new CPU(4,cluster);
        Collection<CPU> CPUS= new ArrayList<>();
        CPUS.add(cpu);
        Collection<GPU> GPUS= new ArrayList<>();
        GPUS.add(GPU);
        cluster.addCPUSandGPUS(GPUS,CPUS);
        model= new Model("test", new Data(Data.Type.Images, 3000));
        ArrayList<Model> m= new ArrayList<Model>();
        m.add(model);
        model.setStudent(new Student("s","sc", Student.Degree.PhD,m));
        GPU.setModel(model);
        cluster.addProcessed(new DataBatch(model.getData(),0),GPU);
    }

    @Test
    public void testModel(){
        GPU.testModel(model);
        assertTrue(GPU.getModel().getResult() == Model.Result.Good ||
                GPU.getModel().getResult() == Model.Result.Bad);
        assertSame(GPU.getModel().getStatus(), Model.Status.Tested);
    }

    @Test
    public void updateTick() {
        int currTick = GPU.getCurrTick();
        receiveDataBatchFromCluster(); //else it will get it during update tick and will start training- which will make currTick=0
        GPU.updateTick();
        System.out.println(GPU.getCurrTick());
        assertEquals(GPU.getCurrTick(), currTick + 1);
    }

    @Test
    public void prepareDataBatches() { //called from constructor
        int size = GPU.getModel().getData().getSize();
        GPU.prepareDataBatches();
        assertEquals(GPU.getUnprocessedDBs().size(),size/1000);
    }

    @Test
    public void sendDataBatchesToCluster(){
        GPU.prepareDataBatches();
        int before = GPU.getUnprocessedDBs().size();
        GPU.sendDataBatchesToCluster();
        assertEquals(GPU.getUnprocessedDBs().size(), before - 1);
    }

    @Test
    public void receiveDataBatchFromCluster() {
        assertTrue(GPU.getProcessedDBs().size() < GPU.getMaxNumOfProcessedBatches());
        int before = GPU.getProcessedDBs().size();
        GPU.receiveDataBatchFromCluster();
        assertEquals(GPU.getProcessedDBs().size(), before + 1);
        assertTrue(GPU.getProcessedDBs().size() <= GPU.getMaxNumOfProcessedBatches());
    }

    @Test
    public void trainModel() {
        GPU.trainModel(model);
        assertEquals(model.getStatus(), Model.Status.Training);
    }


    @Test
    public void startTraining() {
        GPU.receiveDataBatchFromCluster();
        GPU.startTraining();
        assertNotNull(GPU.getCurrDBInTraining());
        assertEquals(GPU.getCurrTick(),0);
        assertEquals(GPU.getStatus(), bgu.spl.mics.application.objects.GPU.GPUStatus.Training);
    }

    @Test
    public void doneTraining() {
        int numBefore= GPU.numOfTrainedDBs;
        int numOfProcessedInModelBefore = GPU.getModel().getData().getProcessed();
        GPU.trainModel(model);
        GPU.startTraining();
        DataBatch curr = GPU.getCurrDBInTraining();
        while ( GPU.getCurrTick() < GPU.getTicksUntilDone()){
            assertSame(curr, GPU.getCurrDBInTraining());
            GPU.setCurrTick(GPU.getCurrTick() +1);
        }
        GPU.doneTraining(); //here CurrTick == TicksUntilDone
        assertNotSame(curr, GPU.getCurrDBInTraining());
        assertEquals(GPU.getNumOfTrainedDBs(),numBefore + 1);
        assertEquals(GPU.getModel().getData().getProcessed(), numOfProcessedInModelBefore + 1000);

    }

    @Test
    public void complete() {
        GPU.complete();
        assertEquals(GPU.getModel().getStatus(), Model.Status.Trained);
        assertEquals(GPU.getStatus(), bgu.spl.mics.application.objects.GPU.GPUStatus.Completed);
    }

}