package org.apache.aries.tx.control.resource.common.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.osgi.framework.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TrackingResourceProviderFactory<T extends AutoCloseable> {

	private static final Logger LOG = LoggerFactory.getLogger(TrackingResourceProviderFactory.class);
	
	private final List<T> toClose = new ArrayList<>();
	
	private boolean closed;
	
	protected T doGetResult(Callable<T> getter) {
		synchronized (getter) {
			if (closed) {
				throw new IllegalStateException("This ResourceProvider has been reclaimed because the factory service that provided it was released");
			}
		}
		T t;
		try {
			t = getter.call();
		} catch (Exception e) {
			LOG.warn("A failure occurred obtaining the resource provider", e);
			throw new ServiceException("A failure occurred obtaining the resource provider", e);
		}
		boolean destroy = false;
		synchronized (toClose) {
			if (closed) {
				destroy = true;
			} else {
			    toClose.add(t);
			}
		}
		if(destroy) {
			try {
				t.close();
			} catch (Exception e) {
				LOG.warn("A failure occurred closing the resource provider", e);
			}
			throw new IllegalStateException("This ResourceProvider has been reclaimed because the factory service that provided it was released");
		}
		return t;
	}

	public void closeAll() {
		synchronized (toClose) {
			closed = true;
		}
		// toClose is now up to date and no other thread will write it
		toClose.stream().forEach(ajcp -> {
			try {
				ajcp.close();
			} catch (Exception e) {}
		});
		
		toClose.clear();
	}
}