package org.apache.aries.cdi.test.cases;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class CloseableTracker<S, T> extends ServiceTracker<S, T> implements AutoCloseable {

	@SuppressWarnings("unused")
	private final StackTraceElement[] caller;

	public CloseableTracker(BundleContext context, Filter filter) {
		super(context, filter, null);
		caller = new Exception().getStackTrace();
	}

	public CloseableTracker(BundleContext context, Filter filter, ServiceTrackerCustomizer<S, T> customizer) {
		super(context, filter, customizer);
		caller = new Exception().getStackTrace();
	}

	@Override
	public void close() {
		super.close();
	}

}
