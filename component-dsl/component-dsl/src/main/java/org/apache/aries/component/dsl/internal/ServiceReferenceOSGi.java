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

package org.apache.aries.component.dsl.internal;

import org.apache.aries.component.dsl.Refresher;
import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.component.dsl.Publisher;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Carlos Sierra Andrés
 */
public class ServiceReferenceOSGi<T>
	extends OSGiImpl<CachingServiceReference<T>> {

	public ServiceReferenceOSGi(
		String filterString, Class<T> clazz) {

		this(filterString, clazz, CachingServiceReference::isDirty);
	}

	public ServiceReferenceOSGi(
		String filterString, Class<T> clazz,
		Refresher<? super CachingServiceReference<T>> refresher) {

		super((bundleContext, op) -> {
			ServiceTracker<T, ?>
				serviceTracker = new ServiceTracker<>(
					bundleContext,
					buildFilter(bundleContext, filterString, clazz),
					new DefaultServiceTrackerCustomizer<>(op, refresher));

			serviceTracker.open();

			return new OSGiResultImpl(serviceTracker::close);
		});
	}

	private static class DefaultServiceTrackerCustomizer<T>
		implements ServiceTrackerCustomizer<T, Tracked<T>> {

		public DefaultServiceTrackerCustomizer(
			Publisher<? super CachingServiceReference<T>> addedSource,
			Refresher<? super CachingServiceReference<T>> refresher) {

			_addedSource = addedSource;
			_refresher = refresher;
		}

		@Override
		public Tracked<T> addingService(ServiceReference<T> reference) {
			CachingServiceReference<T> cachingServiceReference =
				new CachingServiceReference<>(reference);

			return new Tracked<>(
				cachingServiceReference,
				_addedSource.apply(cachingServiceReference));
		}

		@Override
		public void modifiedService(
			ServiceReference<T> reference, Tracked<T> tracked) {

			if (_refresher.test(tracked.cachingServiceReference)) {
				UpdateSupport.runUpdate(() -> {
					tracked.runnable.run();
					tracked.cachingServiceReference = new CachingServiceReference<>(
						reference);
					tracked.runnable =
						_addedSource.apply(tracked.cachingServiceReference);
				});
			}
		}

		@Override
		public void removedService(
			ServiceReference<T> reference, Tracked<T> tracked) {

			tracked.runnable.run();
		}

		private final Publisher<? super CachingServiceReference<T>> _addedSource;
		private Refresher<? super CachingServiceReference<T>> _refresher;

	}

	private static class Tracked<T> {

		public Tracked(
			CachingServiceReference<T> cachingServiceReference,
			Runnable runnable) {

			this.cachingServiceReference = cachingServiceReference;
			this.runnable = runnable;
		}

		volatile CachingServiceReference<T> cachingServiceReference;
		volatile Runnable runnable;

	}
}
