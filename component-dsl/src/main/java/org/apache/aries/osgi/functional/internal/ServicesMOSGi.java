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

import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class ServicesMOSGi<T> extends MOSGiImpl<T> {

	private final String _filterString;

	private final Class<T> _clazz;

	public ServicesMOSGi(Class<T> clazz, String filterString) {
		super(bundleContext -> {
			Pipe<Tuple<T>, Tuple<T>> added = Pipe.create();

			Pipe<Tuple<T>, Tuple<T>> removed = Pipe.create();

			Consumer<Tuple<T>> addedSource = added.getSource();

			Consumer<Tuple<T>> removedSource = removed.getSource();

			ServiceTracker<T, Tuple<T>> serviceTracker =
				new ServiceTracker<>(
					bundleContext,
					OSGiImpl.buildFilter(
						bundleContext, filterString, clazz),
					new ServiceTrackerCustomizer<T, Tuple<T>>() {
						@Override
						public Tuple<T> addingService(
							ServiceReference<T> reference) {

							ServiceObjects<T> serviceObjects =
								bundleContext.getServiceObjects(reference);

							T service = serviceObjects.getService();

							Tuple<T> tuple = Tuple.create(service);

							addedSource.accept(tuple);

							return tuple;
						}

						@Override
						public void modifiedService(
							ServiceReference<T> reference,
							Tuple<T> service) {

							removedService(reference, service);

							addingService(reference);
						}

						@Override
						public void removedService(
							ServiceReference<T> reference, Tuple<T> tuple) {

							ServiceObjects<T> serviceObjects =
								bundleContext.getServiceObjects(reference);

							removedSource.accept(tuple);

							serviceObjects.ungetService(tuple.t);
						}
					});

			return new OSGiResultImpl<>(
				added, removed, serviceTracker::open,
				serviceTracker::close);
		});

		_filterString = filterString;

		_clazz = clazz;
	}

	@Override
	public <S> OSGiImpl<S> flatMap(Function<T, OSGi<S>> fun) {
		return new OSGiImpl<>(bundleContext -> {
			Pipe<Tuple<S>, Tuple<S>> added = Pipe.create();

			Pipe<Tuple<S>, Tuple<S>> removed = Pipe.create();

			Consumer<Tuple<S>> addedSource = added.getSource();

			Consumer<Tuple<S>> removedSource = removed.getSource();

			ServiceTracker<T, Tracked<T, S>> serviceTracker =
				new ServiceTracker<>(
					bundleContext,
					buildFilter(
						bundleContext, _filterString, _clazz),
					new ServiceTrackerCustomizer<T, Tracked<T, S>>() {
						@Override
						public Tracked<T, S> addingService(
							ServiceReference<T> reference) {

							ServiceObjects<T> serviceObjects =
								bundleContext.getServiceObjects(
									reference);

							T service = serviceObjects.getService();

							OSGi<S> program = fun.apply(service);

							Tracked<T, S> tracked = new Tracked<>();

							OSGiResult<S> result = program.run(
								bundleContext, s -> {
									Tuple<S> tuple = Tuple.create(s);

									tracked.result = tuple;

									addedSource.accept(tuple);
								}
							);

							tracked.service = service;
							tracked.program = result;

							return tracked;
						}

						@Override
						public void modifiedService(
							ServiceReference<T> reference,
							Tracked<T, S> tracked) {

							removedService(reference, tracked);

							addingService(reference);
						}

						@Override
						public void removedService(
							ServiceReference<T> reference,
							Tracked<T, S> tracked) {

							tracked.program.close();

							if (tracked.result != null) {
								removedSource.accept(tracked.result);
							}

							ServiceObjects<T> serviceObjects =
								bundleContext.getServiceObjects(
									reference);

							serviceObjects.ungetService(
								tracked.service);
						}
					});

			return new OSGiResultImpl<>(
				added, removed, serviceTracker::open,
				serviceTracker::close);

		});
	}

}
