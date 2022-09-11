package bgu.spl.mics;
import static org.junit.Assert.*;

import bgu.spl.mics.example.messages.ExampleBroadcast;
import bgu.spl.mics.example.messages.ExampleEvent;
import bgu.spl.mics.example.services.ExampleMicroService;
import org.junit.Assert;
import  org.junit.Before;
import  org.junit.After;
import  org.junit.Test;

import java.util.concurrent.TimeUnit;

public class MessageBusTest {
    private static MessageBusImpl mb;
    private static MicroService ms;

    @Before
    public void setUp(){
        mb = MessageBusImpl.getInstance();
        mb.clear();
        ms = new ExampleMicroService("name");
        mb.register(ms);
    }

    @Test
    public void subscribeEvent(){
        Assert.assertFalse(mb.isMicroServiceSubscribedEvent(ms, ExampleEvent.class));
        mb.subscribeEvent(ExampleEvent.class,ms);
        Assert.assertTrue(mb.isMicroServiceSubscribedEvent(ms, ExampleEvent.class));

    }

    @Test
    public void subscribeBroadcast(){
        assertFalse(mb.isMicroServiceSubscribedBroadcast(ms, ExampleBroadcast.class));
        mb.subscribeBroadcast(ExampleBroadcast.class,ms);
        assertTrue(mb.isMicroServiceSubscribedBroadcast(ms, ExampleBroadcast.class));
    }

    @Test
    public void Complete() {
        mb.subscribeEvent(ExampleEvent.class,ms);
        Future<String> futureObject = mb.sendEvent(new ExampleEvent("test"));
        try {
            ExampleEvent message = (ExampleEvent) mb.awaitMessage(ms);
            mb.complete(message,"result");
            Assert.assertTrue(futureObject.isDone());
            Assert.assertEquals(futureObject.get(), "result");
        } catch (InterruptedException ignored) {
        }
    }

    @Test
    public void sendBroadcast() {
        int before = mb.numOfBroadcastsSent();
        ExampleBroadcast b= new ExampleBroadcast("");
        mb.subscribeBroadcast(b.getClass(),ms);
        mb.sendBroadcast(b);
        assertEquals(mb.numOfBroadcastsSent(), before + 1);
        try{
            assertEquals(mb.awaitMessage(ms),b);
        }
        catch (InterruptedException ignored){}
    }


    @Test
    public void sendEvent() {
        int before = mb.numOfEventsSent();
        ExampleEvent e= new ExampleEvent("");
        //assertNull(mb.sendEvent(e));
        mb.subscribeEvent(e.getClass(),ms);
        mb.sendEvent(e);
        assertEquals(mb.numOfEventsSent(), before + 1);
        try{
            assertEquals(mb.awaitMessage(ms),e);
        }
        catch (InterruptedException ignored){}
    }

    @Test
    public void register() {
        Assert.assertTrue(mb.isMicroServiceRegistered(ms));
    }

    @Test
    public void unregister() {
        mb.unregister(ms);
        Assert.assertFalse(mb.isMicroServiceRegistered(ms));
    }

    @Test
    public void awaitMessage() {
        mb.unregister(ms);
        boolean flag = false;
        try {
            mb.awaitMessage(ms);
        } catch (InterruptedException ignored) {
        }catch (IllegalStateException e){
            flag = true;
        }
        Assert.assertTrue(flag);

        mb.register(ms);
        mb.subscribeEvent(ExampleEvent.class,ms);
        mb.sendEvent(new ExampleEvent(""));
        try{
            Message m = mb.awaitMessage(ms);
            Assert.assertTrue(m instanceof ExampleEvent);
        }catch(InterruptedException ignored){}

    }
    @After
    public void TearDown(){
        mb.unregister(ms);
    }

}