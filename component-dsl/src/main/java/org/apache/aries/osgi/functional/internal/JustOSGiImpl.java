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
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Carlos Sierra Andrés
 */
public class JustOSGiImpl<T> extends OSGiImpl<T> {

	private Supplier<T> _t;

	public JustOSGiImpl(Supplier<T> t) {
		super(((bundleContext) -> {

			Pipe<Tuple<T>, Tuple<T>> added = Pipe.create();

			Tuple<T> tuple = Tuple.create(t.get());

			return new OSGiResultImpl<>(
				added, () -> added.getSource().accept(tuple), tuple::terminate);
		}));

		_t = t;
	}

	public JustOSGiImpl(T t) {
		this(() -> t);
	}

	@Override
	public <S> OSGiImpl<S> flatMap(Function<? super T, OSGi<? extends S>> fun) {
		return new OSGiImpl<>(bundleContext -> {
			Pipe<Tuple<S>, Tuple<S>> added = Pipe.create();

			AtomicReference<Runnable> atomicReference = new AtomicReference<>(
				NOOP);

			return new OSGiResultImpl<>(
				added,
				() -> {
					OSGiImpl<S> next = (OSGiImpl<S>) fun.apply(_t.get());

					OSGiResultImpl<S> osGiResult = next._operation.run(
						bundleContext);

					atomicReference.set(osGiResult::close);

					osGiResult.pipeTo(added.getSource());
				},
				() -> {
					atomicReference.get().run();
				});
		});
	}

}
