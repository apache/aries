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
			AtomicReference<Runnable> closeReference =
				new AtomicReference<>(NOOP);

			Pipe<Tuple<S>, Tuple<S>> added = Pipe.create();

			Consumer<Tuple<S>> addedSource = added.getSource();

			return new OSGiResultImpl<>(
				added,
				() -> {
					OSGiResultImpl<T> or1 = previous._operation.run(
						bundleContext);

					closeReference.set(or1.close);

					or1.added.map(t -> {
						OSGiImpl<S> program = (OSGiImpl<S>)fun.apply(t.t);

						OSGiResultImpl<S> or2 =
							program._operation.run(bundleContext);

						t.onTermination(or2::close);

						or2.pipeTo(addedSource);

						return null;
					});

					or1.start.run();
				},
				() -> closeReference.get().run());
			}
		);
	}

}


