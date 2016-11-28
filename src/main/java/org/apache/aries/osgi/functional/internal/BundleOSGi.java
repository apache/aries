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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class BundleOSGi extends OSGiImpl<Bundle> {

	private final int _stateMask;

	public BundleOSGi(int stateMask) {
		super(bundleContext -> {
			Pipe<Tuple<Bundle>, Tuple<Bundle>> added = Pipe.create();

			Consumer<Tuple<Bundle>> addedSource = added.getSource();

			Pipe<Tuple<Bundle>, Tuple<Bundle>> removed = Pipe.create();

			Consumer<Tuple<Bundle>> removedSource = removed.getSource();

			BundleTracker<Tuple<Bundle>> bundleTracker =
				new BundleTracker<>(
					bundleContext, stateMask,
					new BundleTrackerCustomizer<Tuple<Bundle>>() {

						@Override
						public Tuple<Bundle> addingBundle(
							Bundle bundle, BundleEvent bundleEvent) {

							Tuple<Bundle> tuple = Tuple.create(bundle);

							addedSource.accept(tuple);

							return tuple;
						}

						@Override
						public void modifiedBundle(
							Bundle bundle, BundleEvent bundleEvent,
							Tuple<Bundle> tuple) {

							removedBundle(bundle, bundleEvent, tuple);

							addingBundle(bundle, bundleEvent);
						}

						@Override
						public void removedBundle(
							Bundle bundle, BundleEvent bundleEvent,
							Tuple<Bundle> tuple) {

							removedSource.accept(tuple);
						}
					});

			return new OSGiResultImpl<>(
				added, removed, bundleTracker::open, bundleTracker::close);
		});
		_stateMask = stateMask;
	}

	@Override
	public <S> OSGiImpl<S> flatMap(Function<? super Bundle, OSGi<? extends S>> fun) {
		return new OSGiImpl<>(bundleContext -> {
			Pipe<Tuple<S>, Tuple<S>> added = Pipe.create();

			Consumer<Tuple<S>> addedSource = added.getSource();

			Pipe<Tuple<S>, Tuple<S>> removed = Pipe.create();

			Consumer<Tuple<S>> removedSource = removed.getSource();

			BundleTracker<Tracked<Bundle, S>> bundleTracker =
				new BundleTracker<>(
					bundleContext, _stateMask,
					new BundleTrackerCustomizer<Tracked<Bundle, S>>() {

						@Override
						public Tracked<Bundle, S> addingBundle(
							Bundle bundle, BundleEvent bundleEvent) {

							OSGiImpl<S> program = (OSGiImpl<S>) fun.apply(
								bundle);

							OSGiResultImpl<S> result =
								program._operation.run(bundleContext);

							Tracked<Bundle, S> tracked = new Tracked<>();

							tracked.service = bundle;
							tracked.program = result;

							result.added.map(s -> {
								tracked.result = s;

								addedSource.accept(s);

								return s;
							});

							result.start.run();

							return tracked;
						}

						@Override
						public void modifiedBundle(
							Bundle bundle, BundleEvent bundleEvent,
							Tracked<Bundle, S> tracked) {

							removedBundle(bundle, bundleEvent, tracked);

							addingBundle(bundle, bundleEvent);
						}

						@Override
						public void removedBundle(
							Bundle bundle, BundleEvent bundleEvent,
							Tracked<Bundle, S> tracked) {

							tracked.program.close();

							if (tracked.result != null) {
								removedSource.accept(tracked.result);
							}
						}
					});

			return new OSGiResultImpl<>(
				added, removed, bundleTracker::open, bundleTracker::close);

		});
	}

}
