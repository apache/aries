package org.apache.aries.subsystem.core.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemEvent;
import org.apache.aries.subsystem.SubsystemListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this event dispatcher dispatches subsystem events to subsystem listeners.
 *
 */
public class SubsystemEventDispatcher implements SubsystemListener, SynchronousBundleListener {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SubsystemEventDispatcher.class);
    private final Set<SubsystemListener> listeners = new CopyOnWriteArraySet<SubsystemListener>();
    private final Map<Subsystem, SubsystemEvent> states = new ConcurrentHashMap<Subsystem, SubsystemEvent>();
    private final ExecutorService executor = Executors
            .newSingleThreadExecutor();
    private final ServiceTracker containerListenerTracker;

    SubsystemEventDispatcher(final BundleContext bundleContext) {

        assert bundleContext != null;

        this.containerListenerTracker = new ServiceTracker(bundleContext,
                SubsystemListener.class.getName(),
                new ServiceTrackerCustomizer() {
                    public Object addingService(ServiceReference reference) {
                        SubsystemListener listener = (SubsystemListener) bundleContext
                                .getService(reference);

                        synchronized (listeners) {
                            sendInitialEvents(listener);
                            listeners.add(listener);
                        }

                        return listener;
                    }

                    public void modifiedService(ServiceReference reference,
                            Object service) {
                    }

                    public void removedService(ServiceReference reference,
                            Object service) {
                        listeners.remove(service);
                        bundleContext.ungetService(reference);
                    }
                });
        this.containerListenerTracker.open();
    }

    private void sendInitialEvents(SubsystemListener listener) {
        for (Map.Entry<Subsystem, SubsystemEvent> entry : states.entrySet()) {
            try {
                callListener(listener, entry.getValue());
            } catch (RejectedExecutionException ree) {
                LOGGER.warn("Executor shut down", ree);
                break;
            }
        }
    }

    private void callListeners(SubsystemEvent event) {
        for (final SubsystemListener listener : listeners) {
            try {
                callListener(listener, event);
            } catch (RejectedExecutionException ree) {
                LOGGER.warn("Executor shut down", ree);
                break;
            }
        }
    }

    private void callListener(final SubsystemListener listener,
            final SubsystemEvent event) throws RejectedExecutionException {
        try {
            executor.invokeAny(Collections
                    .<Callable<Void>> singleton(new Callable<Void>() {
                        public Void call() throws Exception {
                            listener.subsystemEvent(event);
                            return null;
                        }
                    }), 60L, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            LOGGER.warn("Thread interrupted", ie);
            Thread.currentThread().interrupt();
        } catch (TimeoutException te) {
            LOGGER.warn("Listener timed out, will be ignored", te);
            listeners.remove(listener);
        } catch (ExecutionException ee) {
            LOGGER.warn("Listener caused an exception, will be ignored", ee);
            listeners.remove(listener);
        }
    }

    public void subsystemEvent(SubsystemEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending Subsystem event {} for subsystem {}",
                    toString(event), event.getSubsystem().getSymbolicName());
        }

        synchronized (listeners) {
            callListeners(event);
            states.put(event.getSubsystem(), event);
        }
    }

    @SuppressWarnings( { "ThrowableResultOfMethodCallIgnored" })
    private static String toString(SubsystemEvent event) {
        return "SubsystemEvent[subsystem=" + event.getSubsystem().getSymbolicName() + "/"
                + event.getSubsystem().getVersion()
                + " type=" + event.getType() + "]";
    }

    void destroy() {
        executor.shutdown();
        // wait for the queued tasks to execute
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
        containerListenerTracker.close();
    }

    public void bundleChanged(BundleEvent event) {
        if (BundleEvent.STOPPING == event.getType()) {
            states.remove(event.getBundle());
        }
    }
}
