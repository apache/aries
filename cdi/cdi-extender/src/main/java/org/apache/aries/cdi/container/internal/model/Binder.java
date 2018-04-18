package org.apache.aries.cdi.container.internal.model;

import org.osgi.framework.ServiceReference;

public interface Binder<T> {

	public Binder<T> addingService(ServiceReference<T> reference);
	public Binder<T> modifiedService(ServiceReference<T> reference);
	public Binder<T> removedService(ServiceReference<T> reference);

}
