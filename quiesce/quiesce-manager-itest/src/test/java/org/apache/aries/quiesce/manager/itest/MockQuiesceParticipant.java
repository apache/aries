package org.apache.aries.quiesce.manager.itest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.osgi.framework.Bundle;

public class MockQuiesceParticipant implements QuiesceParticipant {

	public static final int RETURNIMMEDIATELY = 0;
	public static final int NEVERRETURN = 1;
	public static final int WAIT = 2;
	private int behaviour;
	private List<QuiesceCallback> callbacks = new ArrayList<QuiesceCallback>();
	private ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Test");
            t.setDaemon(true);
            return t;
        }
    });
	private int started = 0;
	private int finished = 0;
	
	public MockQuiesceParticipant( int i ) {
		behaviour = i;
	}

	public void quiesce(final QuiesceCallback callback, final List<Bundle> bundlesToQuiesce) {
		Runnable command = new Runnable() {
			public void run() {
				started += 1;
				callbacks.add(callback);
				switch (behaviour) {
				case 0:
					//return immediately
					System.out.println("MockParticipant: return immediately");
					finished += 1;
					callback.bundleQuiesced(bundlesToQuiesce.toArray(new Bundle[bundlesToQuiesce.size()]));
					callbacks.remove(callback);
					break;
				case 1:
					//just don't do anything
					System.out.println("MockParticipant: just don't do anything");
					break;
				case 2:
					//Wait for 1s then quiesce
					System.out.println("MockParticipant: Wait for 1s then quiesce");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
					finished += 1;
					callback.bundleQuiesced(bundlesToQuiesce.toArray(new Bundle[bundlesToQuiesce.size()]));
					callbacks.remove(callback);
					break;
				default: 
					//Unknown behaviour, don't do anything
				}
			}
		};
		executor.execute(command);
	}

	public int getStartedCount() {
		return started;
	}
	
	public int getFinishedCount() {
		return finished;
	}
	
	public synchronized void reset() {
		started = 0;
		finished = 0;
	}
	
	
}
