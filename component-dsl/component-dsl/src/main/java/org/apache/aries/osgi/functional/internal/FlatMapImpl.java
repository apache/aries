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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class FlatMapImpl<T, S> extends OSGiImpl<S> {

	public FlatMapImpl(
		OSGiImpl<T> previous, Function<? super T, OSGi<? extends S>> fun) {

		super((bundleContext) -> {
			Map<IdentityKey<Object>, OSGiResult<?>> identities =
				new ConcurrentHashMap<>();

			AtomicReference<Runnable> closeReference =
				new AtomicReference<>(NOOP);

			Pipe<Tuple<S>, Tuple<S>> added = Pipe.create();

			Consumer<Tuple<S>> addedSource = added.getSource();

			Pipe<Tuple<S>, Tuple<S>> removed = Pipe.create();

			Consumer<Tuple<S>> removedSource = removed.getSource();

			return new OSGiResultImpl<>(
				added, removed,
				() -> {
					OSGiResultImpl<T> or1 = previous._operation.run(
						bundleContext);

					closeReference.set(or1.close);

					or1.added.map(t -> {
						OSGiImpl<S> program = (OSGiImpl<S>)fun.apply(t.t);

						OSGiResultImpl<S> or2 =
							program._operation.run(bundleContext);

						or2.pipeTo(addedSource, removedSource);

						identities.put(new IdentityKey<>(t.original), or2);

						return null;
					});

					or1.removed.map(t -> {
						OSGiResult<?> osgiResult1 = identities.remove(
							new IdentityKey<>(t.original));

						if (osgiResult1 != null) {
							osgiResult1.close();
						}

						return null;
					});

					or1.start.run();
				},
				() -> {
					identities.values().forEach(OSGiResult::close);

					closeReference.get().run();
				});
			}
		);
	}

	private static class IdentityKey<T> {

		private final T _instance;

		public IdentityKey(T instance) {
			_instance = instance;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			IdentityKey<?> that = (IdentityKey<?>) o;

			return _instance == that._instance;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(_instance);
		}

	}

}


