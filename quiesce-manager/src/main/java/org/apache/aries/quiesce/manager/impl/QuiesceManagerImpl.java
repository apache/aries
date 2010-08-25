/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.quiesce.manager.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.manager.QuiesceManager;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuiesceManagerImpl implements QuiesceManager {
	
	/** Logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(QuiesceManagerImpl.class.getName());
    /** The default timeout to use */
    private static int defaultTimeout = 60000; 
    /** The container's {@link BundleContext} */
    private BundleContext bundleContext = null;
    /** The thread pool to execute timeout commands */
    private ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(10, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Quiesce Manager Timeout Thread");
            t.setDaemon(true);
            return t;
        }
    });
    
    /** The thread pool to execute quiesce commands */
    private ExecutorService executor = new ThreadPoolExecutor(0, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),new ThreadFactory() {
		
		public Thread newThread(Runnable arg0) {
			Thread t = new Thread(arg0, "Quiesce Manager Thread");
			t.setDaemon(true);
			return t;
		}
	});
    
    /** The map of bundles that are currently being quiesced */
    private static ConcurrentHashMap<Bundle, Bundle> bundleMap = new ConcurrentHashMap<Bundle, Bundle>();


    public QuiesceManagerImpl(BundleContext bc) {
    	bundleContext = bc;
    }
    
    /**
     * Attempts to quiesce all bundles in the list. After the timeout has elapsed, 
     * or if successfully quiesced before that, the bundles are stopped. This method 
     * is non-blocking. Calling objects wishing to track the state of the bundles 
     * need to listen for the resulting stop events. 
     */
    public void quiesce(long timeout, List<Bundle> bundles) {
        quiesceWithFuture(timeout, bundles);
    }
    
    public Future<?> quiesceWithFuture(List<Bundle> bundlesToQuiesce) {
        return quiesceWithFuture(defaultTimeout, bundlesToQuiesce);
    }
    
    public Future<?> quiesceWithFuture(long timeout, List<Bundle> bundles) {
        QuiesceFuture result = new QuiesceFuture();
        if (bundles != null && !!!bundles.isEmpty()) {
            //check that bundle b is not already quiescing
            Iterator<Bundle> it = bundles.iterator();
            Set<Bundle> bundlesToQuiesce = new HashSet<Bundle>();
            while(it.hasNext()) {
                Bundle b = it.next();
                Bundle priorBundle = bundleMap.putIfAbsent(b, b);
                if (priorBundle == null) {
                    bundlesToQuiesce.add(b);
                }else{
                    LOGGER.warn("Already quiescing bundle "+ b.getSymbolicName());
                }
            }
            Runnable command = new BundleQuiescer(bundlesToQuiesce, timeout, result, bundleMap);
            executor.execute(command);
            
            return result;
        } else {
            result.registerDone();
        }
        
        return result;
    }
    
    private static class QuiesceFuture implements Future<Object> {
        private CountDownLatch latch = new CountDownLatch(1);
        
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException("Quiesce operations can be cancelled");
        }

        public Object get() throws InterruptedException, ExecutionException {
            latch.await();
            return null;
        }

        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (!!!latch.await(timeout, unit))
                throw new TimeoutException();
            
            return null;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return latch.getCount() == 0;
        }
        
        public void registerDone() {
            if (!!!isDone()) {
                latch.countDown();
            }
        }
        
    }

    /**
     * Attempts to quiesce all bundles in the list, using the default timeout. 
     * After the timeout has elapsed, or if successfully quiesced before that, 
     * the bundles are stopped. This method is non-blocking. Calling objects 
     * wishing to track the state of the bundles need to listen for the 
     * resulting stop events. 
     */
    public void quiesce(List<Bundle> bundlesToQuiesce) {
    	quiesce(defaultTimeout, bundlesToQuiesce);
    }
  
    /**
     * Stop a bundle that was to be quiesced. This happens either when all the participants
     * are finished or when the timeout has occurred.
     * 
     * The set of all bundles to quiesce is used to track stops, so that they do not occur twice.
     * @param bundleToStop
     * @param bundlesToStop
     * @return
     */
    private static boolean stopBundle(Bundle bundleToStop, Set<Bundle> bundlesToStop) {
    	try {
    	    synchronized (bundlesToStop) {
    	        if (bundlesToStop.remove(bundleToStop)) {
    	            bundleToStop.stop();
    	            bundleMap.remove(bundleToStop);
    	        }
    	    }
    	} catch (BundleException be) {
    		return false;
    	}
    	return true;
    }
    
    private static boolean stillQuiescing(Bundle bundleToStop) {
        return bundleMap.containsKey(bundleToStop);
    }
    

    /**
     * BundleQuiescer is used for each bundle to quiesce. It creates a callback object for each 
     * participant. Well-behaved participants will be non-blocking on their quiesce method.
     * When all callbacks for the participants have completed, this thread will get an 
     * interrupt, so it sleeps until it hits the timeout. When complete it stops the bundle
     * and removes the bundles from the list of those that are being quiesced.
     */
    private class BundleQuiescer implements Runnable {
	  
    	private final Set<Bundle> bundlesToQuiesce;
    	private final long timeout;
    	private final QuiesceFuture future;
    	
    	public BundleQuiescer(Set<Bundle> bundlesToQuiesce, long timeout, QuiesceFuture future, ConcurrentHashMap<Bundle, Bundle> bundleMap) {
    		this.bundlesToQuiesce = new HashSet<Bundle>(bundlesToQuiesce);
    		this.timeout = timeout;
    		this.future = future;
    	}

    	public void run() {
    		try {
				if (bundleContext != null) {
					ServiceReference[] serviceRefs = bundleContext.getServiceReferences(QuiesceParticipant.class.getName(), null);
					if (serviceRefs != null) {
						List<QuiesceParticipant> participants = new ArrayList<QuiesceParticipant>();
						final List<QuiesceCallbackImpl> callbacks = new ArrayList<QuiesceCallbackImpl>();
						List<Bundle> copyOfBundles = new ArrayList<Bundle>(bundlesToQuiesce);
						
						ScheduledFuture<?> timeoutFuture = timeoutExecutor.schedule(new Runnable() {
						    public void run() {
						        LOGGER.warn("Quiesce timed out");
						        synchronized (bundlesToQuiesce) {
						            for (Bundle b : new ArrayList<Bundle>(bundlesToQuiesce)) {
    						            LOGGER.warn("Could not quiesce within timeout, so stopping bundle "+ b.getSymbolicName());
    						            stopBundle(b, bundlesToQuiesce);
						            }
						        }
						        future.registerDone();
						        LOGGER.debug("Quiesce complete");
						    }
						}, timeout, TimeUnit.MILLISECONDS);

						
						//Create callback objects for all participants
						for( ServiceReference sr : serviceRefs ) {
							QuiesceParticipant participant = (QuiesceParticipant) bundleContext.getService(sr);
							participants.add(participant);
							callbacks.add(new QuiesceCallbackImpl(bundlesToQuiesce, callbacks, future, timeoutFuture));
						}
						
						//Quiesce each participant and wait for an interrupt from a callback 
						//object when all are quiesced, or the timeout to be reached
						for( int i=0; i<participants.size(); i++ ) {
							QuiesceParticipant participant = participants.get(i);
							QuiesceCallbackImpl callback = callbacks.get(i);
							participant.quiesce(callback, copyOfBundles);
						}                        
                    }else{
                        LOGGER.warn("No quiesce participants, so stopping bundles");
                        for (Bundle b : bundlesToQuiesce) {
                            stopBundle(b, bundlesToQuiesce);
                        }
                        future.registerDone();
                    }
                }
            } catch (InvalidSyntaxException e) {
                LOGGER.warn("Exception trying to get service references for quiesce participants, so stopping bundles."+ e.getMessage());
                for (Bundle b : bundlesToQuiesce) {
                    stopBundle(b, bundlesToQuiesce);
                }
                future.registerDone();
            }
        }
	}
 
    /**
     * Callback object provided for each participant for each quiesce call 
     * from the quiesce manager. 
     */
    private static class QuiesceCallbackImpl implements QuiesceCallback {
    	//Must be a copy
    	private final Set<Bundle> toQuiesce;
    	// Must not be a copy
    	private final Set<Bundle> toQuiesceShared;    	
    	//Must not be a copy
    	private final List<QuiesceCallbackImpl> allCallbacks;
    	//Timer so we can cancel the alarm if all done
    	private final QuiesceFuture future;
    	//The cleanup action that runs at timeout
    	private final ScheduledFuture<?> timeoutFuture;
    	
    	public QuiesceCallbackImpl(Set<Bundle> toQuiesce, List<QuiesceCallbackImpl> allCallbacks, QuiesceFuture future, ScheduledFuture<?> timeoutFuture) 
    	{
    		this.toQuiesce = new HashSet<Bundle>(toQuiesce);
    		this.toQuiesceShared = toQuiesce;
    		this.allCallbacks = allCallbacks;
    		this.future = future;
    		this.timeoutFuture = timeoutFuture;
    	}

		/** 
    	 * Removes the bundles from the list of those to quiesce. 
    	 * If the list is now empty, this callback object is finished (i.e. 
    	 * the participant linked to this object has quiesced all the bundles
    	 * requested).  
    	 * 
    	 * If all other participants have also completed, then the 
    	 * calling BundleQuieser thread is interrupted.
    	 */
    	public void bundleQuiesced(Bundle... bundlesQuiesced) {
    		
            boolean timeoutOccurred = false; 
            
            synchronized (allCallbacks) {
                for(Bundle b : bundlesQuiesced) {
                    if(QuiesceManagerImpl.stillQuiescing(b)) {
                        if(toQuiesce.remove(b)) {
                            if(checkOthers(b)){
                                QuiesceManagerImpl.stopBundle(b, toQuiesceShared);
                                if(allCallbacksComplete()){
                                    future.registerDone();
                                    timeoutFuture.cancel(false);
                                    LOGGER.debug("Quiesce complete");
                                }
                            }
                        }
                    } else {
                        timeoutOccurred = true;
                        break;
                    }
                }
                if (timeoutOccurred) {
                        Iterator<QuiesceCallbackImpl> it = allCallbacks.iterator();
                        while (it.hasNext()) {
                            it.next().toQuiesce.clear();
                        }
                }
			}
    	}

		private boolean checkOthers(Bundle b) {
			boolean allDone = true;
			Iterator<QuiesceCallbackImpl> it = allCallbacks.iterator();
			while (allDone && it.hasNext()) {
				allDone = !!!it.next().toQuiesce.contains(b);
			}
			return allDone;
		}
		
		private boolean allCallbacksComplete() {
			boolean allDone = true;
			Iterator<QuiesceCallbackImpl> it = allCallbacks.iterator();
			while (allDone && it.hasNext()) {
                QuiesceCallbackImpl next = it.next();
                if (!!!next.toQuiesce.isEmpty()) allDone = false;
			}
			return allDone;
		}		
    }
}