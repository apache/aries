/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.async.impl;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.apache.aries.async.promise.PromiseImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.async.delegate.AsyncDelegate;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;


public class MethodCall {
	
	private final Bundle clientBundle;
	private final ServiceTracker<LogService, LogService> logServiceTracker;
	
	private final ServiceReference<?> reference;
	private final Object service;

	final Method method;
	final Object[] arguments;
	
	public MethodCall(Bundle clientBundle, ServiceTracker<LogService, LogService> logServiceTracker, 
			ServiceReference<?> reference, Object service, Method method, Object[] arguments) {
		this.clientBundle = clientBundle;
		this.logServiceTracker = logServiceTracker;
		this.reference = reference;
		this.service = service;
		this.method = method;
		this.arguments = arguments;
	}

	Object getService() {
		if(reference != null) {
			BundleContext bc = clientBundle.getBundleContext();
			if(bc != null) {
				try {
					Object svc = bc.getService(reference);
					if(svc == null) {
						throw new ServiceException("Unable to retrieve the mediated service because it has been unregistered", 7);
					} else {
						return svc;
					}
				} catch (Exception e) {
					throw new ServiceException("Unable to retrieve the mediated service", 7, e);
				}
			} else {
				throw new ServiceException("Unable to retrieve the mediated service because the client bundle has been stopped", 7);
			}
		} else {
			return service;
		}
	}
	
	void releaseService() {
		if(reference != null) {
			BundleContext bc = clientBundle.getBundleContext();
			if(bc != null) {
				bc.ungetService(reference);
			}
		}
	}
	
	public <V> Promise<V> invokeAsynchronously(Bundle clientBundle, ExecutorService executor) {
		
		PromiseImpl<V> promiseImpl = new PromiseImpl<V>(executor);

		Object svc;
		try {
			svc = getService();
		} catch (Exception e) {
			promiseImpl.fail(e);
			return promiseImpl;
		}
		
		if(svc instanceof AsyncDelegate) {
			try {
				@SuppressWarnings("unchecked")
				Promise<V> p = (Promise<V>) ((AsyncDelegate) svc).async(method, arguments);
				if(p != null) {
					try {
						promiseImpl.resolveWith(p);
						return promiseImpl;
					} finally {
						releaseService();
					}
				}
			} catch (Exception e) {
				try {
					promiseImpl.fail(e);
					return promiseImpl;
				} finally {
					releaseService();
				}
			}
		}
		//If we get here then svc is either not an async delegate, or it rejected the call
		
		try {
			executor.execute(new Work<V>(this, promiseImpl));
		} catch (RejectedExecutionException ree) {
			promiseImpl.fail(new ServiceException("The Async service is unable to accept new requests", 7, ree));
		}
		
		//Release the service we got at the start of this method
		promiseImpl.onResolve(new Runnable() {
			public void run() {
				releaseService();
			}
		});
		
		return promiseImpl;
	}

	public Promise<Void> fireAndForget(Bundle clientBundle, ExecutorService executor) {
		PromiseImpl<Void> started = new PromiseImpl<Void>(executor);
		Object svc;
		try {
			svc = getService();
		} catch (Exception e) {
			logError("Unable to obtain the service object", e);
			started.fail(e);
			return started;
		}
		
		if(svc instanceof AsyncDelegate) {
			try {
				if(((AsyncDelegate) svc).execute(method, arguments)) {
					releaseService();
					started.resolve(null);
					return started;
				}
			} catch (Exception e) {
				releaseService();
				logError("The AsyncDelegate rejected the fire-and-forget invocation with an exception", e);
				started.fail(e);
				return started;
			}
		}
		//If we get here then svc is either not an async delegate, or it rejected the call
		
		PromiseImpl<Void> cleanup = new PromiseImpl<Void>();
		try {
			executor.execute(new FireAndForgetWork(this, cleanup, started));
			cleanup.onResolve(new Runnable() {
				public void run() {
					releaseService();
				}
			}).then(null, new Failure(){
				public void fail(Promise<?> resolved) throws Exception {
					logError("The fire-and-forget invocation failed", resolved.getFailure());
				}
			});
		} catch (RejectedExecutionException ree) {
			logError("The Async Service threadpool rejected the fire-and-forget invocation", ree);
			started.fail(new ServiceException("Unable to enqueue the fire-and forget task", 7, ree));
		}
		return started;
	}

	void logError(String message, Throwable e) {
		for(LogService log : logServiceTracker.getServices(new LogService[0])) {
			if(reference == null) {
				log.log(LogService.LOG_ERROR, message, e);
			} else {
				log.log(reference,  LogService.LOG_ERROR, message, e);
			}
		}
	}
}
