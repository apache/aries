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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * @author Carlos Sierra Andrés
 */
public class BundleOSGi extends OSGiImpl<Bundle> {

	public BundleOSGi(int stateMask) {
		super((bundleContext, op) -> {
			BundleTracker<Runnable> bundleTracker =
				new BundleTracker<>(
					bundleContext, stateMask,
					new BundleTrackerCustomizer<Runnable>() {

						@Override
						public Runnable addingBundle(
							Bundle bundle, BundleEvent bundleEvent) {

							return op.apply(bundle);
						}

						@Override
						public void modifiedBundle(
							Bundle bundle, BundleEvent bundleEvent,
							Runnable runnable) {

						}

						@Override
						public void removedBundle(
							Bundle bundle, BundleEvent bundleEvent,
							Runnable runnable) {

							runnable.run();
						}
					});

			bundleTracker.open();

			return new OSGiResultImpl(bundleTracker::close);
		});

	}

}
