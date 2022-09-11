package bgu.spl.mics;

import bgu.spl.mics.example.messages.ExampleEvent;
import bgu.spl.mics.example.services.ExampleMicroService;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class FutureTest {
    private static MessageBusImpl mb;
    private static MicroService ms;
    private static Future<String> future;
    private static ExampleEvent e;

    @Before
    public void setUp(){
        //creates event, future for that event, messageBus and MicroService
        e = new ExampleEvent("test");
        mb = MessageBusImpl.getInstance();
        mb.clear();
        ms = new ExampleMicroService("name");
        mb.register(ms);
        mb.subscribeEvent(e.getClass(),ms);
        future = mb.sendEvent(e);
    }

    @Test
    public void get() {
        AtomicBoolean flag = new AtomicBoolean(false);
        Thread t = new Thread(() -> {future.resolve("result"); flag.set(true);});
        t.start();
        try{
            t.join();
        }catch (InterruptedException ignore){}
        String result = future.get();

        assertTrue("result".equals(result) && flag.get()); //checks that the result was placed only when future has been resolved
    }

    @Test
    public void resolve() {
        future.resolve("result");
        assertEquals("result", future.get());
    }

    @Test
    public void isDone() {
        future.resolve("result");
        assertTrue(future.isDone());
    }

    @Test
    public void testGet() {
        long timeout = 3L;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        future = mb.sendEvent(e);
        LocalTime beforeTime = LocalTime.now();
        assertNull(future.get(timeout, timeUnit)); //if future is not resolved should to return null
        LocalTime afterTime = LocalTime.now();
        assertTrue(beforeTime.until(afterTime, ChronoUnit.MILLIS) < 3700); //checks that the get took <timeout> seconds
        future.resolve("result");
        assertEquals(future.get(timeout,timeUnit),"result");
    }
}