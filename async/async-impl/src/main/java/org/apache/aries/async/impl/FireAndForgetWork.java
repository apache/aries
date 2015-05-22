package org.apache.aries.async.impl;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.util.promise.Deferred;

public class FireAndForgetWork implements Runnable {

	private final MethodCall methodCall;
	
	private final Deferred<Void> cleanup;
	private final Deferred<Void> started;

	private final AccessControlContext acc;
	
	public FireAndForgetWork(MethodCall methodCall, Deferred<Void> cleanup, Deferred<Void> started) {
		this.methodCall = methodCall;
		this.cleanup = cleanup;
		this.started = started;
		this.acc = AccessController.getContext();
	}


	public void run() {
		try {
			final Object service = methodCall.getService();
			// This is necessary for non public methods. The original mediator call must
			// have been allowed to happen, so this should always be safe.
			methodCall.method.setAccessible(true);
			
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				public Void run() {
					started.resolve(null);
					try {
						methodCall.method.invoke(service, methodCall.arguments);
						cleanup.resolve(null);
					} catch (InvocationTargetException ite) {
						cleanup.fail(ite.getTargetException());
					} catch (Exception e) {
						cleanup.fail(e);
					}
					return null;
				}
			}, acc);
		} catch (Exception e) {
			started.fail(e);
			cleanup.fail(e);
		} finally {
			methodCall.releaseService();
		}
	}
}
