package bgu.spl.mics.application.services;

import bgu.spl.mics.MessageBus;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminationBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;

/**
 * TimeService is the global system timer There is only one instance of this micro-service.
 * It keeps track of the amount of ticks passed since initialization and notifies
 * all other micro-services about the current time tick using {@link TickBroadcast}.
 * This class may not hold references for objects which it is not responsible for.
 * 
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class TimeService extends MicroService{
	private int speed;
	private int duration;
	private int totalTicks;
	private Timer timer;


	public TimeService(int speed, int duration) {
		super("Time Service");
		this.speed= speed;
		this.duration= duration;
		timer= new Timer();
		totalTicks=0;
	}

	@Override
	protected void initialize() {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				sendBroadcast(new TickBroadcast());
				totalTicks++;
				if(totalTicks==duration){
					sendBroadcast(new TerminationBroadcast());
					timer.cancel();
				}
			}
		}, 0,speed); //0?

		subscribeBroadcast(TerminationBroadcast.class, c -> terminate());
	}


}
