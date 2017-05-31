/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.aries.util.tracker.hook;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The Tracked and AbstractTracked inner classes are copied from felix framework 4.0.1.
 *
 * @version $Rev$ $Date$
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BundleHookBundleTracker<T> extends BundleTracker {

    static {
        Class c = EventHook.class;
    }

    /* set this to true to compile in debug messages */
    static final boolean				DEBUG	= false;

    /**
     * The Bundle Context used by this {@code BundleTracker}.
     */
    protected final BundleContext context;

    /**
     * The {@code BundleTrackerCustomizer} object for this tracker.
     */
    final BundleTrackerCustomizer customizer;
    /**
     * Tracked bundles: {@code Bundle} object -> customized Object and
     * {@code BundleListener} object
     */
    private volatile Tracked tracked;

    /**
     * Accessor method for the current Tracked object. This method is only
     * intended to be used by the unsynchronized methods which do not modify the
     * tracked field.
     *
     * @return The current Tracked object.
     */
    private Tracked tracked() {
        return tracked;
    }

    /**
     * State mask for bundles being tracked. This field contains the ORed values
     * of the bundle states being tracked.
     */
    private final int mask;

    /**
     * BundleHook service registration
     */
    private ServiceRegistration sr;

    /**
     * Create a {@code BundleTracker} for bundles whose state is present in the
     * specified state mask.
     *
     * <p>
     * Bundles whose state is present on the specified state mask will be
     * tracked by this {@code BundleTracker}.
     *
     * @param context The {@code BundleContext} against which the tracking is
     *        done.
     * @param stateMask The bit mask of the {@code OR}ing of the bundle states
     *        to be tracked.
     * @param customizer The customizer object to call when bundles are added,
     *        modified, or removed in this {@code BundleTracker}. If customizer
     *        is {@code null}, then this {@code BundleTracker} will be used as
     *        the {@code BundleTrackerCustomizer} and this {@code BundleTracker}
     *        will call the {@code BundleTrackerCustomizer} methods on itself.
     * @see Bundle#getState()
     */
    public BundleHookBundleTracker(BundleContext context, int stateMask, BundleTrackerCustomizer customizer) {
        super(context, stateMask, customizer);
        this.context = context;
        this.mask = stateMask;
        this.customizer = customizer == null ? this : customizer;
    }

    /**
     * Open this {@code BundleTracker} and begin tracking bundles.
     *
     * <p>
     * Bundle which match the state criteria specified when this
     * {@code BundleTracker} was created are now tracked by this
     * {@code BundleTracker}.
     *
     * @throws java.lang.IllegalStateException If the {@code BundleContext} with
     *         which this {@code BundleTracker} was created is no longer valid.
     * @throws java.lang.SecurityException If the caller and this class do not
     *         have the appropriate
     *         {@code AdminPermission[context bundle,LISTENER]}, and the Java
     *         Runtime Environment supports permissions.
     */
    @Override
    public void open() {
        final Tracked t;
        synchronized (this) {
            if (tracked != null) {
                return;
            }
            t = new Tracked();
            synchronized (t) {
                EventHook hook = new BundleEventHook(t);
                sr = context.registerService(EventHook.class.getName(), hook, null);
                Bundle[] bundles = context.getBundles();
                if (bundles != null) {
                    int length = bundles.length;
                    for (int i = 0; i < length; i++) {
                        int state = bundles[i].getState();
                        if ((state & mask) == 0) {
                            /* null out bundles whose states are not interesting */
                            bundles[i] = null;
                        }
                    }
                    /* set tracked with the initial bundles */
                    t.setInitial(bundles);
                }
            }
            tracked = t;
        }
        /* Call tracked outside of synchronized region */
        tracked.trackInitial(); /* process the initial references */
    }

    /**
     * Close this {@code BundleTracker}.
     *
     * <p>
     * This method should be called when this {@code BundleTracker} should end
     * the tracking of bundles.
     *
     * <p>
     * This implementation calls {@link #getBundles()} to get the list of
     * tracked bundles to remove.
     */
    @Override
    public void close() {
        final Bundle[] bundles;
        final Tracked outgoing;
        synchronized (this) {
            outgoing = tracked;
            if (outgoing == null) {
                return;
            }
            if (DEBUG) {
                System.out.println("BundleTracker.close"); //$NON-NLS-1$
            }
            tracked.close();
            bundles = getBundles();
            tracked = null;
            try {
                sr.unregister();
            } catch (IllegalStateException e) {
				/* In case the context was stopped. */
            }
        }
        if (bundles != null) {
            for (int i = 0; i < bundles.length; i++) {
                outgoing.untrack(bundles[i], null);
            }
        }
    }

    /**
     * Default implementation of the
     * {@code BundleTrackerCustomizer.addingBundle} method.
     *
     * <p>
     * This method is only called when this {@code BundleTracker} has been
     * constructed with a {@code null BundleTrackerCustomizer} argument.
     *
     * <p>
     * This implementation simply returns the specified {@code Bundle}.
     *
     * <p>
     * This method can be overridden in a subclass to customize the object to be
     * tracked for the bundle being added.
     *
     * @param bundle The {@code Bundle} being added to this
     *        {@code BundleTracker} object.
     * @param event The bundle event which caused this customizer method to be
     *        called or {@code null} if there is no bundle event associated with
     *        the call to this method.
     * @return The specified bundle.
     * @see BundleTrackerCustomizer#addingBundle(Bundle, BundleEvent)
     */
    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        T result = (T) bundle;
        return result;
    }

    /**
     * Default implementation of the
     * {@code BundleTrackerCustomizer.modifiedBundle} method.
     *
     * <p>
     * This method is only called when this {@code BundleTracker} has been
     * constructed with a {@code null BundleTrackerCustomizer} argument.
     *
     * <p>
     * This implementation does nothing.
     *
     * @param bundle The {@code Bundle} whose state has been modified.
     * @param event The bundle event which caused this customizer method to be
     *        called or {@code null} if there is no bundle event associated with
     *        the call to this method.
     * @param object The customized object for the specified Bundle.
     * @see BundleTrackerCustomizer#modifiedBundle(Bundle, BundleEvent, Object)
     */
    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
		/* do nothing */
    }

    /**
     * Default implementation of the
     * {@code BundleTrackerCustomizer.removedBundle} method.
     *
     * <p>
     * This method is only called when this {@code BundleTracker} has been
     * constructed with a {@code null BundleTrackerCustomizer} argument.
     *
     * <p>
     * This implementation does nothing.
     *
     * @param bundle The {@code Bundle} being removed.
     * @param event The bundle event which caused this customizer method to be
     *        called or {@code null} if there is no bundle event associated with
     *        the call to this method.
     * @param object The customized object for the specified bundle.
     * @see BundleTrackerCustomizer#removedBundle(Bundle, BundleEvent, Object)
     */
    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        /* do nothing */
    }

    /**
     * Return an array of {@code Bundle}s for all bundles being tracked by this
     * {@code BundleTracker}.
     *
     * @return An array of {@code Bundle}s or {@code null} if no bundles are
     *         being tracked.
     */
    public Bundle[] getBundles() {
        final Tracked t = tracked();
        if (t == null) { /* if BundleTracker is not open */
            return null;
        }
        synchronized (t) {
            int length = t.size();
            if (length == 0) {
                return null;
            }
            return t.copyKeys(new Bundle[length]);
        }
    }

    /**
     * Returns the customized object for the specified {@code Bundle} if the
     * specified bundle is being tracked by this {@code BundleTracker}.
     *
     * @param bundle The {@code Bundle} being tracked.
     * @return The customized object for the specified {@code Bundle} or
     *         {@code null} if the specified {@code Bundle} is not being
     *         tracked.
     */
    public T getObject(Bundle bundle) {
        final Tracked t = tracked();
        if (t == null) { /* if BundleTracker is not open */
            return null;
        }
        synchronized (t) {
            return t.getCustomizedObject(bundle);
        }
    }

    /**
     * Remove a bundle from this {@code BundleTracker}.
     *
     * The specified bundle will be removed from this {@code BundleTracker} . If
     * the specified bundle was being tracked then the
     * {@code BundleTrackerCustomizer.removedBundle} method will be called for
     * that bundle.
     *
     * @param bundle The {@code Bundle} to be removed.
     */
    public void remove(Bundle bundle) {
        final Tracked t = tracked();
        if (t == null) { /* if BundleTracker is not open */
            return;
        }
        t.untrack(bundle, null);
    }

    /**
     * Return the number of bundles being tracked by this {@code BundleTracker}.
     *
     * @return The number of bundles being tracked.
     */
    public int size() {
        final Tracked t = tracked();
        if (t == null) { /* if BundleTracker is not open */
            return 0;
        }
        synchronized (t) {
            return t.size();
        }
    }

    /**
     * Returns the tracking count for this {@code BundleTracker}.
     *
     * The tracking count is initialized to 0 when this {@code BundleTracker} is
     * opened. Every time a bundle is added, modified or removed from this
     * {@code BundleTracker} the tracking count is incremented.
     *
     * <p>
     * The tracking count can be used to determine if this {@code BundleTracker}
     * has added, modified or removed a bundle by comparing a tracking count
     * value previously collected with the current tracking count value. If the
     * value has not changed, then no bundle has been added, modified or removed
     * from this {@code BundleTracker} since the previous tracking count was
     * collected.
     *
     * @return The tracking count for this {@code BundleTracker} or -1 if this
     *         {@code BundleTracker} is not open.
     */
    public int getTrackingCount() {
        final Tracked t = tracked();
        if (t == null) { /* if BundleTracker is not open */
            return -1;
        }
        synchronized (t) {
            return t.getTrackingCount();
        }
    }

    /**
     * Return a {@code Map} with the {@code Bundle}s and customized objects for
     * all bundles being tracked by this {@code BundleTracker}.
     *
     * @return A {@code Map} with the {@code Bundle}s and customized objects for
     *         all services being tracked by this {@code BundleTracker}. If no
     *         bundles are being tracked, then the returned map is empty.
     * @since 1.5
     */
    public Map<Bundle, T> getTracked() {
        Map<Bundle, T> map = new HashMap<Bundle, T>();
        final Tracked t = tracked();
        if (t == null) { /* if BundleTracker is not open */
            return map;
        }
        synchronized (t) {
            return t.copyEntries(map);
        }
    }

    /**
     * Return if this {@code BundleTracker} is empty.
     *
     * @return {@code true} if this {@code BundleTracker} is not tracking any
     *         bundles.
     * @since 1.5
     */
    public boolean isEmpty() {
        final Tracked t = tracked();
        if (t == null) { /* if BundleTracker is not open */
            return true;
        }
        synchronized (t) {
            return t.isEmpty();
        }
    }

    private class BundleEventHook implements EventHook {
        private final Tracked tracked;

        private BundleEventHook(Tracked tracked) {
            this.tracked = tracked;
        }

        public void event(BundleEvent bundleEvent, Collection bundleContexts) {
            tracked.bundleChanged(bundleEvent);
        }
    }

    /**
     * Inner class which subclasses AbstractTracked. This class is the
     * {@code SynchronousBundleListener} object for the tracker.
     *
     * @ThreadSafe
     * @since 1.4
     */
    private final class Tracked extends AbstractTracked<Bundle, T, BundleEvent> implements SynchronousBundleListener {
        /**
         * Tracked constructor.
         */
        Tracked() {
            super();
        }

        /**
         * {@code BundleListener} method for the {@code BundleTracker}
         * class. This method must NOT be synchronized to avoid deadlock
         * potential.
         *
         * @param event {@code BundleEvent} object from the framework.
         */
        public void bundleChanged(final BundleEvent event) {
            /*
             * Check if we had a delayed call (which could happen when we
             * close).
             */
            if (closed) {
                return;
            }
            final Bundle bundle = event.getBundle();
            final int state = bundle.getState();
            if (DEBUG) {
                System.out.println("BundleTracker.Tracked.bundleChanged[" + state + "]: " + bundle); //$NON-NLS-1$ //$NON-NLS-2$
            }

            if ((state & mask) != 0) {
                track(bundle, event);
                /*
                 * If the customizer throws an unchecked exception, it is safe
                 * to let it propagate
                 */
            } else {
                untrack(bundle, event);
                /*
                 * If the customizer throws an unchecked exception, it is safe
                 * to let it propagate
                 */
            }
        }

        /**
         * Call the specific customizer adding method. This method must not be
         * called while synchronized on this object.
         *
         * @param item    Item to be tracked.
         * @param related Action related object.
         * @return Customized object for the tracked item or {@code null}
         *         if the item is not to be tracked.
         */
        T customizerAdding(final Bundle item, final BundleEvent related) {
            return (T) customizer.addingBundle(item, related);
        }

        /**
         * Call the specific customizer modified method. This method must not be
         * called while synchronized on this object.
         *
         * @param item    Tracked item.
         * @param related Action related object.
         * @param object  Customized object for the tracked item.
         */
        void customizerModified(final Bundle item, final BundleEvent related,
                                final T object) {
            customizer.modifiedBundle(item, related, object);
        }

        /**
         * Call the specific customizer removed method. This method must not be
         * called while synchronized on this object.
         *
         * @param item    Tracked item.
         * @param related Action related object.
         * @param object  Customized object for the tracked item.
         */
        void customizerRemoved(final Bundle item, final BundleEvent related,
                               final T object) {
            customizer.removedBundle(item, related, object);
        }
    }

    /**
     * Abstract class to track items. If a Tracker is reused (closed then reopened),
     * then a new AbstractTracked object is used. This class acts a map of tracked
     * item -> customized object. Subclasses of this class will act as the listener
     * object for the tracker. This class is used to synchronize access to the
     * tracked items. This is not a public class. It is only for use by the
     * implementation of the Tracker class.
     *
     * @param <S> The tracked item. It is the key.
     * @param <T> The value mapped to the tracked item.
     * @param <R> The reason the tracked item is being tracked or untracked.
     * @version $Id: 79452e6c28683021f2bcf11d3689ec75c6b5642f $
     * @ThreadSafe
     * @since 1.4
     */
    private static abstract class AbstractTracked<S, T, R> {
        /* set this to true to compile in debug messages */
        static final boolean DEBUG = false;

        /**
         * Map of tracked items to customized objects.
         *
         * @GuardedBy this
         */
        private final Map<S, T> tracked;

        /**
         * Modification count. This field is initialized to zero and incremented by
         * modified.
         *
         * @GuardedBy this
         */
        private int trackingCount;

        /**
         * List of items in the process of being added. This is used to deal with
         * nesting of events. Since events may be synchronously delivered, events
         * can be nested. For example, when processing the adding of a service and
         * the customizer causes the service to be unregistered, notification to the
         * nested call to untrack that the service was unregistered can be made to
         * the track method.
         * <p/>
         * Since the ArrayList implementation is not synchronized, all access to
         * this list must be protected by the same synchronized object for
         * thread-safety.
         *
         * @GuardedBy this
         */
        private final List<S> adding;

        /**
         * true if the tracked object is closed.
         * <p/>
         * This field is volatile because it is set by one thread and read by
         * another.
         */
        volatile boolean closed;

        /**
         * Initial list of items for the tracker. This is used to correctly process
         * the initial items which could be modified before they are tracked. This
         * is necessary since the initial set of tracked items are not "announced"
         * by events and therefore the event which makes the item untracked could be
         * delivered before we track the item.
         * <p/>
         * An item must not be in both the initial and adding lists at the same
         * time. An item must be moved from the initial list to the adding list
         * "atomically" before we begin tracking it.
         * <p/>
         * Since the LinkedList implementation is not synchronized, all access to
         * this list must be protected by the same synchronized object for
         * thread-safety.
         *
         * @GuardedBy this
         */
        private final LinkedList<S> initial;

        /**
         * AbstractTracked constructor.
         */
        AbstractTracked() {
            tracked = new HashMap<S, T>();
            trackingCount = 0;
            adding = new ArrayList<S>(6);
            initial = new LinkedList<S>();
            closed = false;
        }

        /**
         * Set initial list of items into tracker before events begin to be
         * received.
         * <p/>
         * This method must be called from Tracker's open method while synchronized
         * on this object in the same synchronized block as the add listener call.
         *
         * @param list The initial list of items to be tracked. {@code null}
         *             entries in the list are ignored.
         * @GuardedBy this
         */
        void setInitial(S[] list) {
            if (list == null) {
                return;
            }
            for (S item : list) {
                if (item == null) {
                    continue;
                }
                if (DEBUG) {
                    System.out.println("AbstractTracked.setInitial: " + item); //$NON-NLS-1$
                }
                initial.add(item);
            }
        }

        /**
         * Track the initial list of items. This is called after events can begin to
         * be received.
         * <p/>
         * This method must be called from Tracker's open method while not
         * synchronized on this object after the add listener call.
         */
        void trackInitial() {
            while (true) {
                S item;
                synchronized (this) {
                    if (closed || (initial.size() == 0)) {
                        /*
                         * if there are no more initial items
                         */
                        return; /* we are done */
                    }
                    /*
                    * move the first item from the initial list to the adding list
                    * within this synchronized block.
                    */
                    item = initial.removeFirst();
                    if (tracked.get(item) != null) {
                        /* if we are already tracking this item */
                        if (DEBUG) {
                            System.out.println("AbstractTracked.trackInitial[already tracked]: " + item); //$NON-NLS-1$
                        }
                        continue; /* skip this item */
                    }
                    if (adding.contains(item)) {
                        /*
                         * if this item is already in the process of being added.
                         */
                        if (DEBUG) {
                            System.out.println("AbstractTracked.trackInitial[already adding]: " + item); //$NON-NLS-1$
                        }
                        continue; /* skip this item */
                    }
                    adding.add(item);
                }
                if (DEBUG) {
                    System.out.println("AbstractTracked.trackInitial: " + item); //$NON-NLS-1$
                }
                trackAdding(item, null); /*
                                          * Begin tracking it. We call trackAdding
									      * since we have already put the item in the
									      * adding list.
									      */
            }
        }

        /**
         * Called by the owning Tracker object when it is closed.
         */
        void close() {
            closed = true;
        }

        /**
         * Begin to track an item.
         *
         * @param item    Item to be tracked.
         * @param related Action related object.
         */
        void track(final S item, final R related) {
            final T object;
            synchronized (this) {
                if (closed) {
                    return;
                }
                object = tracked.get(item);
                if (object == null) { /* we are not tracking the item */
                    if (adding.contains(item)) {
                        /* if this item is already in the process of being added. */
                        if (DEBUG) {
                            System.out
                                    .println("AbstractTracked.track[already adding]: " + item); //$NON-NLS-1$
                        }
                        return;
                    }
                    adding.add(item); /* mark this item is being added */
                } else { /* we are currently tracking this item */
                    if (DEBUG) {
                        System.out
                                .println("AbstractTracked.track[modified]: " + item); //$NON-NLS-1$
                    }
                    modified(); /* increment modification count */
                }
            }

            if (object == null) { /* we are not tracking the item */
                trackAdding(item, related);
            } else {
                /* Call customizer outside of synchronized region */
                customizerModified(item, related, object);
                /*
                 * If the customizer throws an unchecked exception, it is safe to
                 * let it propagate
                 */
            }
        }

        /**
         * Common logic to add an item to the tracker used by track and
         * trackInitial. The specified item must have been placed in the adding list
         * before calling this method.
         *
         * @param item    Item to be tracked.
         * @param related Action related object.
         */
        private void trackAdding(final S item, final R related) {
            if (DEBUG) {
                System.out.println("AbstractTracked.trackAdding: " + item); //$NON-NLS-1$
            }
            T object = null;
            boolean becameUntracked = false;
            /* Call customizer outside of synchronized region */
            try {
                object = customizerAdding(item, related);
                /*
                 * If the customizer throws an unchecked exception, it will
                 * propagate after the finally
                 */
            } finally {
                synchronized (this) {
                    if (adding.remove(item) && !closed) {
                        /*
                         * if the item was not untracked during the customizer
                         * callback
                         */
                        if (object != null) {
                            tracked.put(item, object);
                            modified(); /* increment modification count */
                            notifyAll(); /* notify any waiters */
                        }
                    } else {
                        becameUntracked = true;
                    }
                }
            }
            /*
             * The item became untracked during the customizer callback.
             */
            if (becameUntracked && (object != null)) {
                if (DEBUG) {
                    System.out.println("AbstractTracked.trackAdding[removed]: " + item); //$NON-NLS-1$
                }
                /* Call customizer outside of synchronized region */
                customizerRemoved(item, related, object);
                /*
                 * If the customizer throws an unchecked exception, it is safe to
                 * let it propagate
                 */
            }
        }

        /**
         * Discontinue tracking the item.
         *
         * @param item    Item to be untracked.
         * @param related Action related object.
         */
        void untrack(final S item, final R related) {
            final T object;
            synchronized (this) {
                if (initial.remove(item)) { /*
										     * if this item is already in the list
										     * of initial references to process
										     */
                    if (DEBUG) {
                        System.out.println("AbstractTracked.untrack[removed from initial]: " + item); //$NON-NLS-1$
                    }
                    return; /*
						     * we have removed it from the list and it will not be
						     * processed
						     */
                }

                if (adding.remove(item)) { /*
										    * if the item is in the process of
										    * being added
										    */
                    if (DEBUG) {
                        System.out.println("AbstractTracked.untrack[being added]: " + item); //$NON-NLS-1$
                    }
                    return; /*
						     * in case the item is untracked while in the process of
						     * adding
						     */
                }
                object = tracked.remove(item); /*
											    * must remove from tracker before
											    * calling customizer callback
											    */
                if (object == null) { /* are we actually tracking the item */
                    return;
                }
                modified(); /* increment modification count */
            }
            if (DEBUG) {
                System.out.println("AbstractTracked.untrack[removed]: " + item); //$NON-NLS-1$
            }
            /* Call customizer outside of synchronized region */
            customizerRemoved(item, related, object);
            /*
             * If the customizer throws an unchecked exception, it is safe to let it
             * propagate
             */
        }

        /**
         * Returns the number of tracked items.
         *
         * @return The number of tracked items.
         * @GuardedBy this
         */
        int size() {
            return tracked.size();
        }

        /**
         * Returns if the tracker is empty.
         *
         * @return Whether the tracker is empty.
         * @GuardedBy this
         * @since 1.5
         */
        boolean isEmpty() {
            return tracked.isEmpty();
        }

        /**
         * Return the customized object for the specified item
         *
         * @param item The item to lookup in the map
         * @return The customized object for the specified item.
         * @GuardedBy this
         */
        T getCustomizedObject(final S item) {
            return tracked.get(item);
        }

        /**
         * Copy the tracked items into an array.
         *
         * @param list An array to contain the tracked items.
         * @return The specified list if it is large enough to hold the tracked
         *         items or a new array large enough to hold the tracked items.
         * @GuardedBy this
         */
        S[] copyKeys(final S[] list) {
            return tracked.keySet().toArray(list);
        }

        /**
         * Increment the modification count. If this method is overridden, the
         * overriding method MUST call this method to increment the tracking count.
         *
         * @GuardedBy this
         */
        void modified() {
            trackingCount++;
        }

        /**
         * Returns the tracking count for this {@code ServiceTracker} object.
         * <p/>
         * The tracking count is initialized to 0 when this object is opened. Every
         * time an item is added, modified or removed from this object the tracking
         * count is incremented.
         *
         * @return The tracking count for this object.
         * @GuardedBy this
         */
        int getTrackingCount() {
            return trackingCount;
        }

        /**
         * Copy the tracked items and associated values into the specified map.
         *
         * @param <M> Type of {@code Map} to hold the tracked items and
         *            associated values.
         * @param map The map into which to copy the tracked items and associated
         *            values. This map must not be a user provided map so that user code
         *            is not executed while synchronized on this.
         * @return The specified map.
         * @GuardedBy this
         * @since 1.5
         */
        <M extends Map<? super S, ? super T>> M copyEntries(final M map) {
            map.putAll(tracked);
            return map;
        }

        /**
         * Call the specific customizer adding method. This method must not be
         * called while synchronized on this object.
         *
         * @param item    Item to be tracked.
         * @param related Action related object.
         * @return Customized object for the tracked item or {@code null} if
         *         the item is not to be tracked.
         */
        abstract T customizerAdding(final S item, final R related);

        /**
         * Call the specific customizer modified method. This method must not be
         * called while synchronized on this object.
         *
         * @param item    Tracked item.
         * @param related Action related object.
         * @param object  Customized object for the tracked item.
         */
        abstract void customizerModified(final S item, final R related,
                                         final T object);

        /**
         * Call the specific customizer removed method. This method must not be
         * called while synchronized on this object.
         *
         * @param item    Tracked item.
         * @param related Action related object.
         * @param object  Customized object for the tracked item.
         */
        abstract void customizerRemoved(final S item, final R related,
                                        final T object);
    }

}
