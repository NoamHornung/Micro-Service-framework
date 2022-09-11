package bgu.spl.mics.application.objects;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import static bgu.spl.mics.application.objects.GPU.Type.RTX2080;
import static org.junit.Assert.*;
import bgu.spl.mics.application.objects.CPU.CPUStatus;

import java.sql.ClientInfoStatus;
import java.util.ArrayList;
import java.util.Collection;

public class CPUTest{

    CPU CPU;
    Cluster cluster;
    @Before
    public void setUp(){
        CPU = new CPU(4, Cluster.getInstance());
        cluster= Cluster.getInstance();
        cluster.clear();
        GPU gpu= new GPU(RTX2080,Cluster.getInstance());
        Collection<CPU> CPUS= new ArrayList<>();
        CPUS.add(CPU);
        Collection<GPU> GPUS= new ArrayList<>();
        GPUS.add(gpu);
        cluster.addCPUSandGPUS(GPUS,CPUS);
        cluster.receiveDataBatchFromGPU(new DataBatch(new Data(Data.Type.Images, 2000),0),gpu);
        cluster.receiveDataBatchFromGPU(new DataBatch(new Data(Data.Type.Tabular, 2000),0),gpu);

    }

    @Test
    public void UpdateTick() {
        int currTick = CPU.getCurrTick();
        CPU.updateTick();//in waiting
        assertEquals(CPU.getCurrTick(), 0); //after start processing
        CPU.updateTick(); //in processing
        assertEquals(CPU.getCurrTick(), currTick + 1);
    }

    @Test
    public void GetDataBatches() {
        int before = CPU.getData().size();
        CPU.getDataBatch();
        assertEquals(CPU.getData().size() , before+1);
    }

    @Test
    public void StartProcessing() {
        CPU.getData().add(new DataBatch(new Data(Data.Type.Images, 2000),0));
        CPU.startProcessing();
        assertNotNull(CPU.getCurrDataBatch());
        Data.Type type = CPU.getCurrDataBatch().getDataType();
        if(type == Data.Type.Images){
            assertEquals(CPU.getTicksUntilDone(),(32/CPU.getCores()) * 4);
        }else if(type == Data.Type.Text){
            assertEquals(CPU.getTicksUntilDone(),(32/CPU.getCores()) * 2);
        }else if(type == Data.Type.Tabular){
            assertEquals(CPU.getTicksUntilDone(),32/CPU.getCores());
        }
        assertEquals(CPU.getCurrTick(),0);
    }
    @Test
    public void SendDataBatch() {
        CPU.getDataBatch();
        int size = CPU.getData().size();
        CPU.sendDataBatch();
        assertEquals(CPU.getData().size(), size - 1);
    }

    @Test
    public void DoneProcessing() {
        CPU.getDataBatch();
        CPU.getDataBatch();
        //check that it did not move to a new batch for <ticksUntilDone> ticks have passed
        CPU.startProcessing();
        DataBatch curr = CPU.getCurrDataBatch();
        while(CPU.getCurrTick() < CPU.getTicksUntilDone()){
            assertSame(CPU.getCurrDataBatch(), curr);
            CPU.setCurrTick(CPU.getCurrTick() +1);
        }
        CPU.doneProcessing();
        assertNotSame(CPU.getCurrDataBatch(), curr); //checks that it started processing a new batch
        assertEquals(1, CPU.getData().size()); //checks that it remove the already processed batch
    }

}