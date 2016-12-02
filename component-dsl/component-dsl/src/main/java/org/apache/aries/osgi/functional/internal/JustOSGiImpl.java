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

			return new OSGiResultImpl<>(
				added, Pipe.create(),
				() -> source.accept(Tuple.create(t)), OSGi.NOOP);
		}));

		_t = t;
	}

	@Override
	public <S> OSGiImpl<S> flatMap(Function<? super T, OSGi<? extends S>> fun) {
		return new OSGiImpl<>(bundleContext -> {
			Pipe<Tuple<S>, Tuple<S>> added = Pipe.create();

			Consumer<Tuple<S>> addedSource = added.getSource();

			AtomicReference<OSGiResult<? extends S>> atomicReference =
				new AtomicReference<>(null);

			return new OSGiResultImpl<>(
				added, Pipe.create(),
				() -> {
					OSGi<? extends S> next = fun.apply(_t);

					atomicReference.set(
						next.run(
							bundleContext,
							s -> addedSource.accept(Tuple.create(s))));

				},
				() -> atomicReference.get().close());
		});
	}
}
