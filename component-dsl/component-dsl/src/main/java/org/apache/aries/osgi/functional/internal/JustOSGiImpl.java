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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class JustOSGiImpl<T> extends OSGiImpl<T> {

	private T _t;

	public JustOSGiImpl(T t) {
		super(((bundleContext) -> {

			Pipe<Tuple<T>, Tuple<T>> added = Pipe.create();

			Consumer<Tuple<T>> source = added.getSource();

			Pipe<Tuple<T>, Tuple<T>> removed = Pipe.create();

			Consumer<Tuple<T>> removedSource = removed.getSource();

			Tuple<T> tuple = Tuple.create(t);

			return new OSGiResultImpl<>(
				added, removed,
				() -> source.accept(tuple),
				() -> removedSource.accept(tuple));
		}));

		_t = t;
	}

	@Override
	public <S> OSGiImpl<S> flatMap(Function<? super T, OSGi<? extends S>> fun) {
		return new OSGiImpl<>(bundleContext -> {
			Pipe<Tuple<S>, Tuple<S>> added = Pipe.create();

			Consumer<Tuple<S>> addedSource = added.getSource();

			Pipe<Tuple<S>, Tuple<S>> removed = Pipe.create();

			Consumer<Tuple<S>> removedSource = removed.getSource();

			AtomicReference<OSGiResult<? extends S>> atomicReference =
				new AtomicReference<>(null);
			AtomicReference<Tuple<S>> tupleReference =
				new AtomicReference<>();

			return new OSGiResultImpl<>(
				added, removed,
				() -> {
					OSGi<? extends S> next = fun.apply(_t);

					atomicReference.set(
						next.run(
							bundleContext,
							s -> {
								Tuple<S> tuple = Tuple.create(s);

								tupleReference.set(tuple);

								addedSource.accept(tuple);
							}));

				},
				() -> {
					removedSource.accept(tupleReference.get());

					atomicReference.get().close();
				});
		});
	}

/*
	@Override
	public <S> OSGi<S> applyTo(OSGi<Function<T, S>> fun) {
		return new OSGiImpl<>(bundleContext -> {
			Pipe<Tuple<S>, Tuple<S>> added = Pipe.create();

			Consumer<Tuple<S>> addedSource = added.getSource();

			Pipe<Tuple<S>, Tuple<S>> removed = Pipe.create();

			Consumer<Tuple<S>> removedSource = removed.getSource();

			IdentityHashMap<Function<T, S>, Tuple<S>> identityMap =
				new IdentityHashMap<>();

			OSGi<Void> next = fun.foreach(
				f -> {
					Tuple<S> tuple = Tuple.create(f.apply(_t));

					identityMap.put(f, tuple);

					addedSource.accept(tuple);
				},
				f -> {
					Tuple<S> tuple = identityMap.remove(f);

					if (tuple != null) {
						removedSource.accept(tuple);
					}
				});

			AtomicReference<OSGiResult<Void>> atomicReference =
				new AtomicReference<>();

			return new OSGiResultImpl<>(
				added, removed,
				() -> atomicReference.set(next.run(bundleContext)),
				() -> {
					identityMap.forEach((f, t) -> removedSource.accept(t));

					identityMap.clear();

					atomicReference.get().close();
				});
		});
	}
*/
}
