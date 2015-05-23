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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.blueprint.supplier.impl;

import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_NAME;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.persistence.EntityManagerFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public class EmfProxy implements InvocationHandler {
	private ServiceTracker<EntityManagerFactory, EntityManagerFactory> tracker;

	public EmfProxy(BundleContext context, String unitName) {
		String filterS = String.format("(&(objectClass=%s)(%s=%s))",
				EntityManagerFactory.class.getName(), JPA_UNIT_NAME, unitName);
		Filter filter;
		try {
			filter = FrameworkUtil.createFilter(filterS);
		} catch (InvalidSyntaxException e) {
			throw new IllegalStateException(e);
		}
		tracker = new ServiceTracker<>(context, filter, null);
		tracker.open();
	}

	private EntityManagerFactory getEntityManagerFactory() {
		try {
			return tracker.waitForService(10000);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		Object res = null;

		EntityManagerFactory delegate = getEntityManagerFactory();

		try {
			res = method.invoke(delegate, args);
		} catch (IllegalArgumentException e) {
			new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			new IllegalStateException(e);
		}
		return res;
	}
}
