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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Carlos Sierra Andrés
 */
public class JustOSGiImpl<T> extends OSGiImpl<T> {

	private Supplier<Collection<T>> _t;

	public JustOSGiImpl(Collection<T> t) {
		this(() -> t);
	}

	public JustOSGiImpl(Supplier<Collection<T>> t) {
		super(((bundleContext) -> {

			Pipe<T, T> added = Pipe.create();

			AtomicReference<Collection<Tuple<T>>> collectionAtomicReference =
				new AtomicReference<>();

			return new OSGiResultImpl<>(
				added,
				() -> {
					List<Tuple<T>> tuples =
						t.get().stream().map(Tuple::create).collect(
							Collectors.toList());

					collectionAtomicReference.set(tuples);

					tuples.forEach(tuple ->
						added.getSource().accept(tuple));
				},
				() ->
					collectionAtomicReference.get().forEach(Tuple::terminate));
		}));

		_t = t;
	}

	public JustOSGiImpl(T t) {
		this(() -> Collections.singletonList(t));
	}

	@Override
	public <S> OSGiImpl<S> flatMap(Function<? super T, OSGi<? extends S>> fun) {
		return new OSGiImpl<>(bundleContext -> {
			Pipe<S, S> added = Pipe.create();

			AtomicReference<Runnable> atomicReference = new AtomicReference<>(
				NOOP);

			return new OSGiResultImpl<>(
				added,
				() -> {
					List<OSGiResultImpl<S>> results = _t.get().stream().map(
						p -> (OSGiImpl<S>) fun.apply(p)
					).map(
						n -> n._operation.run(bundleContext)
					).collect(Collectors.toList());

					atomicReference.set(
						() -> results.forEach(OSGiResult::close));

					results.forEach(result -> result.pipeTo(added.getSource()));
				},
				() -> atomicReference.get().run());
		});
	}

}
