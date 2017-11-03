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

import org.apache.aries.osgi.functional.CachingServiceReference;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class ServiceReferenceOSGi<T>
	extends OSGiImpl<CachingServiceReference<T>> {

	private String _filterString;
	private Class<T> _clazz;

	public ServiceReferenceOSGi(String filterString, Class<T> clazz) {
		super((bundleContext, op) -> {
			ServiceTracker<T, AtomicReference<Runnable>>
				serviceTracker = new ServiceTracker<>(
					bundleContext,
					buildFilter(bundleContext, filterString, clazz),
					new DefaultServiceTrackerCustomizer<>(op));

			return new OSGiResultImpl(
				serviceTracker::open, serviceTracker::close);
		});

		_filterString = filterString;
		_clazz = clazz;
	}

	@Override
	public <S> OSGiImpl<S> flatMap(
		Function<? super CachingServiceReference<T>, OSGi<? extends S>> fun) {

		return new OSGiImpl<>((bundleContext, op) -> {
			ServiceTracker<T, ?> serviceTracker =
				new ServiceTracker<>(
					bundleContext,
					buildFilter(
						bundleContext, _filterString, _clazz),
						new FlatMapServiceTrackerCustomizer<>(
							fun, bundleContext, op));

			return new OSGiResultImpl(
				serviceTracker::open, serviceTracker::close);
		});
	}

	private static class DefaultServiceTrackerCustomizer<T>
		implements ServiceTrackerCustomizer<T, AtomicReference<Runnable>> {

		private final Function<CachingServiceReference<T>, Runnable>
			_addedSource;

		public DefaultServiceTrackerCustomizer(
			Function<CachingServiceReference<T>, Runnable> addedSource) {

			_addedSource = addedSource;
		}

		@Override
		public AtomicReference<Runnable> addingService(
			ServiceReference<T> reference) {

			return new AtomicReference<>(
				_addedSource.apply(new CachingServiceReference<>(reference)));
		}

		@Override
		public void modifiedService(
			ServiceReference<T> reference,
			AtomicReference<Runnable> tupleReference) {

			tupleReference.get().run();

			tupleReference.set(
				_addedSource.apply(new CachingServiceReference<>(reference)));
		}

		@Override
		public void removedService(
			ServiceReference<T> reference,
			AtomicReference<Runnable> runnable) {

			runnable.get().run();
		}
	}

	private static class FlatMapServiceTrackerCustomizer<T, S>
		implements ServiceTrackerCustomizer<T, AtomicReference<OSGiResult>> {
		private final Function<? super CachingServiceReference<T>, OSGi<? extends S>>
			_fun;
		private final BundleContext _bundleContext;
		private final Function<S, Runnable> _op;

		FlatMapServiceTrackerCustomizer(
			Function<? super CachingServiceReference<T>, OSGi<? extends S>> fun,
			BundleContext bundleContext, Function<S, Runnable> op) {

			_fun = fun;
			_bundleContext = bundleContext;
			_op = op;
		}

		@Override
        public AtomicReference<OSGiResult> addingService(
        	ServiceReference<T> reference) {

			OSGiResultImpl osgiResult = doFlatMap(
				new CachingServiceReference<>(reference));

			return new AtomicReference<>(osgiResult);
        }

		private OSGiResultImpl doFlatMap(CachingServiceReference<T> reference) {
			OSGiImpl<S> program = (OSGiImpl<S>) _fun.apply(reference);

			OSGiResultImpl result = program._operation.run(_bundleContext, _op);

			result.start();

			return result;
		}

		@Override
        public void modifiedService(
        	ServiceReference<T> reference,
			AtomicReference<OSGiResult> tracked) {

			tracked.get().close();

			tracked.set(doFlatMap(new CachingServiceReference<>(reference)));
        }

		@Override
        public void removedService(
            ServiceReference<T> reference,
			AtomicReference<OSGiResult> tracked) {

            tracked.get().close();
        }

	}

}
