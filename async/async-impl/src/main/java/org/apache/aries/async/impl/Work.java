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

import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.apache.aries.async.promise.PromiseImpl;

public class Work<T> implements Runnable {

	private final MethodCall methodCall;
	
	private final PromiseImpl<T> promiseImpl;

	private final AccessControlContext acc;
	
	public Work(MethodCall methodCall, PromiseImpl<T> promiseImpl) {
		this.methodCall = methodCall;
		this.promiseImpl = promiseImpl;
		this.acc = AccessController.getContext();
	}


	public void run() {
		try {
			final Object service = methodCall.getService();
			// This is necessary for non public methods. The original mediator call must
			// have been allowed to happen, so this should always be safe.
			methodCall.method.setAccessible(true);
			
			@SuppressWarnings("unchecked")
			T returnValue = AccessController.doPrivileged(new PrivilegedExceptionAction<T>() {
				public T run() throws Exception {
					return (T) methodCall.method.invoke(service, methodCall.arguments);
				}
			}, acc);
			
			promiseImpl.resolve(returnValue);
			
		} catch (PrivilegedActionException pae) {
			Throwable targetException = pae.getCause();
			if(targetException instanceof InvocationTargetException) {
				targetException = ((InvocationTargetException) targetException).getTargetException();
			}
			promiseImpl.fail(targetException);
		} catch (Exception e) {
			promiseImpl.fail(e);
		} finally {
			methodCall.releaseService();
		}
	}
}
