/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.osgi.functional.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.function.Consumer;

/**
 * @author Carlos Sierra Andrés
 */
public class ServiceReferenceOSGi<T>
	extends OSGiImpl<ServiceReference<T>> {

	public ServiceReferenceOSGi(String filterString, Class<T> clazz) {
		super(bundleContext -> {
			Pipe<Tuple<ServiceReference<T>>, Tuple<ServiceReference<T>>>
				added = Pipe.create();

			Consumer<Tuple<ServiceReference<T>>> addedSource =
				added.getSource();

			Pipe<Tuple<ServiceReference<T>>, Tuple<ServiceReference<T>>>
				removed = Pipe.create();

			Consumer<Tuple<ServiceReference<T>>> removedSource =
				removed.getSource();

			ServiceTracker<T, Tuple<ServiceReference<T>>> serviceTracker =
				new ServiceTracker<T, Tuple<ServiceReference<T>>>(
					bundleContext,
					OSGiImpl.buildFilter(
						bundleContext, filterString, clazz), null) {

					@Override
					public Tuple<ServiceReference<T>> addingService(
						ServiceReference<T> reference) {

						Tuple<ServiceReference<T>> tuple = Tuple.create(
							reference);

						addedSource.accept(tuple);

						return tuple;
					}

					@Override
					public void removedService(
						ServiceReference<T> reference,
						Tuple<ServiceReference<T>> t) {

						super.removedService(reference, t);

						removedSource.accept(t);
					}
				};

			return new OSGiResultImpl<>(
				added, removed, serviceTracker::open,
				serviceTracker::close);

		});
	}

}
