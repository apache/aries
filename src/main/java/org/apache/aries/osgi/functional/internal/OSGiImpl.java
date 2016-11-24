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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class OSGiImpl<T> implements OSGi<T> {

	public OSGiOperationImpl<T> _operation;

	public OSGiImpl(OSGiOperationImpl<T> operation) {
		_operation = operation;
	}

	@Override
	public <S> OSGiImpl<S> flatMap(Function<T, OSGi<S>> fun) {
		return new OSGiImpl<>(
			((bundleContext) -> {
				Map<Object, OSGiResult<S>> identities = new IdentityHashMap<>();

				AtomicReference<Runnable> closeReference =
					new AtomicReference<>(NOOP);

				Pipe<Tuple<S>, Tuple<S>> added = Pipe.create();

				Consumer<Tuple<S>> addedSource = added.getSource();

				OSGiResultImpl<S> osgiResult = new OSGiResultImpl<>(
					added, Pipe.create(), null,
					() -> {
						synchronized (identities) {
							identities.values().forEach(OSGiResult::close);
						}

						closeReference.get().run();
					});

				osgiResult.start = () -> {
					OSGiResultImpl<T> or1 = _operation.run(bundleContext);

					closeReference.set(or1.close);

					or1.added.map(t -> {
						OSGi<S> program = fun.apply(t.t);

						OSGiResult<S> or2 = program.run(
							bundleContext,
							s -> addedSource.accept(Tuple.create(s)));

						identities.put(t.original, or2);

						return null;
					});

					or1.removed.map(t -> {
						synchronized (identities) {
							OSGiResult<S> osgiResult1 = identities.remove(
								t.original);

							if (osgiResult1 != null) {
								osgiResult1.close();
							}
						}

						return null;
					});

					or1.start.run();
				};

				return osgiResult;
			}
			));
	}

	@Override
	public <S> OSGi<Void> foreach(Function<T, OSGi<S>> fun) {
		return this.flatMap(fun).map(x -> null);
	}

	@Override
	public <S> OSGi<S> map(Function<T, S> function) {
		return new OSGiImpl<>(((bundleContext) -> {
			OSGiResultImpl<T> osgiResult = _operation.run(bundleContext);

			return new OSGiResultImpl<>(
				osgiResult.added.map(t -> t.map(function)),
				osgiResult.removed.map(t -> t.map(function)),
				osgiResult.start, osgiResult.close);
		}));
	}

	@Override
	public OSGiResult<T> run(BundleContext bundleContext) {
		return run(bundleContext, x -> {});
	}

	@Override
	public OSGiResult<T> run(BundleContext bundleContext, Consumer<T> andThen) {
		OSGiResultImpl<T> osgiResult = _operation.run(bundleContext);

		osgiResult.added.map(x -> {andThen.accept(x.t); return null;});

		osgiResult.start.run();

		return new OSGiResultImpl<>(
			osgiResult.added, osgiResult.removed,
			osgiResult.start, osgiResult.close);
	}

	@Override
	public <S> OSGi<S> then(OSGi<S> next) {
		return flatMap(ignored -> next);
	}

	static Filter buildFilter(
		BundleContext bundleContext, String filterString, Class<?> clazz) {

		Filter filter;

		try {
			if (filterString == null) {
				filter = bundleContext.createFilter(
					"(objectClass=" + clazz.getName() + ")");
			}
			else {
				filter = bundleContext.createFilter(
					"(&(objectClass=" + clazz.getName() + ")" +
						filterString + ")");
			}
		}
		catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}

		return filter;
	}

}


