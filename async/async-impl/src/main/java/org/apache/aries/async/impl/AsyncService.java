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

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.async.Async;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;


public class AsyncService implements Async {

	private static final class CGLibAwareClassLoader extends ClassLoader {
		private final ClassLoader serviceTypeLoader;

		private CGLibAwareClassLoader(Bundle registeringBundle) {
			this.serviceTypeLoader = registeringBundle.adapt(BundleWiring.class).getClassLoader();
		}

		private CGLibAwareClassLoader(ClassLoader loader) {
			this.serviceTypeLoader = loader;
		}

		@Override
		protected Class<?> findClass(String var0)
				throws ClassNotFoundException {
			if(var0.startsWith("net.sf.cglib")) {
				return AsyncService.class.getClassLoader().loadClass(var0);
			} else {
				return serviceTypeLoader.loadClass(var0);
			}
		}
	}

	/**
	 * It is important to use both weak keys *and* values in this map. The
	 * key must be weakly held because it is typically a type from another 
	 * bundle, and would represent a classloader leak if held after that 
	 * bundle was uninstalled. The value must be weak because it either 
	 * extends or implements the type that is the key, and so holds a strong 
	 * reference to the key, which again would cause a leak.
	 * 
	 * This cache may drop the value type if no mediators are held, however in
	 * this situation we can simply create a new value without risking exploding
	 * the heap.
	 */
	private final WeakHashMap<Class<?>, WeakReference<Class<?>>> proxyLoaderCache
		= new WeakHashMap<Class<?>, WeakReference<Class<?>>>();
	
	private final Bundle clientBundle;
	
	private final ConcurrentMap<Thread, MethodCall> invocations = new ConcurrentHashMap<Thread, MethodCall>();
	
	private final ExecutorService executor;
	
	private final ServiceTracker<LogService, LogService> logServiceTracker;
	
	public AsyncService(Bundle clientBundle, ExecutorService executor, ServiceTracker<LogService, LogService> logServiceTracker) {
		super();
		this.clientBundle = clientBundle;
		this.executor = executor;
		this.logServiceTracker = logServiceTracker;
	}
	
	void clear() {
		proxyLoaderCache.clear();
	}

	public <T> T mediate(final T service, final Class<T> iface) {
		return AccessController.doPrivileged(new PrivilegedAction<T>() {
			public T run() {
				return privMediate(service, iface);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private <T> T privMediate(T service, Class<T> iface) {
		
		TrackingInvocationHandler handler = new TrackingInvocationHandler(this, 
				clientBundle, logServiceTracker, service);
		
        synchronized(proxyLoaderCache) {
            T toReturn = cachedMediate(iface, handler);
		
            if(toReturn != null) {
                return toReturn;
            } else if(iface.isInterface()) {
                toReturn = (T) Proxy.newProxyInstance(
					new ClassLoader(service.getClass().getClassLoader()){}, 
					new Class[] {iface}, handler);
            } else {
                toReturn = (T) proxyClass(iface, handler,
					new CGLibAwareClassLoader(service.getClass().getClassLoader()));
            }
            proxyLoaderCache.put(iface, new WeakReference<Class<?>>(toReturn.getClass()));
        
            return toReturn;
        }
	}

	@SuppressWarnings("unchecked")
	private <T> T cachedMediate(Class<T> iface, TrackingInvocationHandler handler) {
		WeakReference<Class<?>> weakReference = proxyLoaderCache.get(iface);
		Class<?> cached = weakReference == null ? null : weakReference.get();
		if(cached != null) {
			if(iface.isInterface()) {
				try {
					return (T) cached.getConstructor(InvocationHandler.class)
							.newInstance(handler);
				} catch (Exception e) {
					throw new IllegalArgumentException("Unable to mediate interface: " + iface, e);
				}
			} else {
                try {
                    T t = (T) cached.getConstructor().newInstance();
                    ((Factory)t).setCallbacks(new Callback[] {handler});
                    return t;
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to mediate class: " + iface, e);
                }
			}
		}
		return null;
	}

	public <T> T mediate(final ServiceReference<? extends T> ref, final Class<T> iface) {
		return AccessController.doPrivileged(new PrivilegedAction<T>() {
			public T run() {
				return privMediate(ref, iface);
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	private <T> T privMediate(ServiceReference<? extends T> ref, Class<T> iface) {

		TrackingInvocationHandler handler = new TrackingInvocationHandler(this, 
				clientBundle, logServiceTracker, ref);
		
        synchronized(proxyLoaderCache) {
            T toReturn = cachedMediate(iface, handler);
            
            if(toReturn != null) {
                return toReturn;
            } else if(iface.isInterface()) {
                toReturn = (T) Proxy.newProxyInstance(
					new ClassLoader(iface.getClassLoader()){}, 
					new Class[] {iface}, handler);
            } else {
                toReturn = (T) proxyClass(iface, handler,
					new CGLibAwareClassLoader(iface.getClassLoader()));
            }
            proxyLoaderCache.put(iface, new WeakReference<Class<?>>(toReturn.getClass()));
            
            return toReturn;
        }
	}

	private Object proxyClass(Class<?> mostSpecificClass, 
			TrackingInvocationHandler handler, ClassLoader classLoader) {
		
		acceptClassType(mostSpecificClass);
		
		Enhancer enhancer = new Enhancer();
		enhancer.setClassLoader(classLoader);
		enhancer.setSuperclass(mostSpecificClass);
		enhancer.setCallback(handler);
		
		return enhancer.create();
	}

	private void acceptClassType(Class<?> type) {
		
		if(Modifier.isFinal(type.getModifiers())) {
			throw new IllegalArgumentException("The type " + type.getName() + " is final");
		}
		try {
			type.getConstructor();
		} catch (NoSuchMethodException nsme) {
			throw new IllegalArgumentException("The type " + type.getName() + " has no zero-argument constructor", nsme);
		}
		
		Class<?> toCheck = type;
		while(toCheck != Object.class) {
			for(Method m : toCheck.getDeclaredMethods()) {
				if(Modifier.isFinal(m.getModifiers())) {
					throw new IllegalArgumentException("The type hierarchy for " + type.getName() + 
							" has a final method " + m.getName() + " defined on " + toCheck.getName());
				}
			}
			toCheck = toCheck.getSuperclass();
		}
	}

	public <T> Promise<T> call(T call) throws IllegalStateException {
		MethodCall currentInvocation = consumeCurrentInvocation();
		if(currentInvocation == null) throw new IllegalStateException("Incorrect API usage - this thread has no pending method calls");
		return currentInvocation.invokeAsynchronously(clientBundle, executor);
	}

	public Promise<?> call() throws IllegalStateException {
		return call(null);
	}

	public Promise<Void> execute() throws IllegalStateException {
		MethodCall currentInvocation = consumeCurrentInvocation();
		if(currentInvocation == null) throw new IllegalStateException("Incorrect API usage - this thread has no pending method calls");
		return currentInvocation.fireAndForget(clientBundle, executor);
	}

	void registerInvocation(MethodCall invocation) {
		if(invocations.putIfAbsent(Thread.currentThread(), invocation) != null) {
			invocations.remove(Thread.currentThread());
			throw new IllegalStateException("Incorrect API usage - this thread already has a pending method call");
		}
	}

	MethodCall consumeCurrentInvocation() {
		return invocations.remove(Thread.currentThread());
	}

}
