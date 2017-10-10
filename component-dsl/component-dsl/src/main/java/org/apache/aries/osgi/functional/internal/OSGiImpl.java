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
import org.apache.aries.osgi.functional.internal.ConcurrentDoublyLinkedList.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Carlos Sierra Andrés
 */
public class OSGiImpl<T> implements OSGi<T> {

	public OSGiOperationImpl<T> _operation;

	public OSGiImpl(OSGiOperationImpl<T> operation) {
		_operation = operation;
	}

	@Override
	public <S> OSGiImpl<S> flatMap(Function<? super T, OSGi<? extends S>> fun) {
		return new FlatMapImpl<>(this, fun);
	}

	@Override
	public OSGi<Void> foreach(Consumer<? super T> onAdded) {
		return foreach(onAdded, ign -> {});
	}

	@Override
	public OSGi<Void> foreach(
		Consumer<? super T> onAdded, Consumer<? super T> onRemoved) {

		return OSGi.ignore(effects(onAdded, onRemoved));
	}

	@Override
	public OSGi<T> effects(
		Consumer<? super T> onAdded, Consumer<? super T> onRemoved) {

		return new OSGiImpl<>((bundleContext, op) ->
			_operation.run(
				bundleContext,
				t -> {
					onAdded.accept(t._t);

					op.accept(t);

					t.onTermination(() -> onRemoved.accept(t._t));
				}));
	}

	@Override
	public <S> OSGi<S> map(Function<? super T, ? extends S> function) {
		return new OSGiImpl<>((bundleContext, op) ->
			_operation.run(bundleContext, t -> op.accept(t.map(function))));
	}

	@Override
	public OSGiResult run(BundleContext bundleContext) {
		return run(bundleContext, x -> {});
	}

	@Override
	public OSGiResult run(BundleContext bundleContext, Consumer<T> andThen) {
		OSGiResultImpl osgiResult =
			_operation.run(bundleContext, t -> andThen.accept(t._t));

		osgiResult.start();

		return osgiResult;
	}

	@Override
	public <S> OSGi<S> then(OSGi<S> next) {
		return flatMap(ignored -> next);
	}

	static Filter buildFilter(
		BundleContext bundleContext, String filterString, Class<?> clazz) {

		Filter filter;

		String string = buildFilterString(filterString, clazz);

		try {
			filter = bundleContext.createFilter(string);
		}
		catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}

		return filter;
	}

	static String buildFilterString(String filterString, Class<?> clazz) {
		if (filterString == null && clazz == null) {
			throw new IllegalArgumentException(
				"Both filterString and clazz can't be null");
		}

		StringBuilder stringBuilder = new StringBuilder();

		if (filterString != null) {
			stringBuilder.append(filterString);
		}

		if (clazz != null) {
			boolean extend = !(stringBuilder.length() == 0);
			if (extend) {
				stringBuilder.insert(0, "(&");
			}

			stringBuilder.
				append("(objectClass=").
				append(clazz.getName()).
				append(")");

			if (extend) {
				stringBuilder.append(")");
			}

		}

		return stringBuilder.toString();
	}

	@Override
	public OSGi<T> filter(Predicate<T> predicate) {
		return flatMap(t -> {
			if (predicate.test(t)) {
				return OSGi.just(t);
			}
			else {
				return OSGi.nothing();
			}
		});
	}

	@Override
	public OSGi<T> route(Consumer<Router<T>> routerConsumer) {
		return new RouteOsgiImpl<>(this, routerConsumer);
	}

	@Override
	public <S> OSGi<S> applyTo(OSGi<Function<T, S>> fun) {
		return new OSGiImpl<>(
			((bundleContext, op) -> {
				AtomicReference<OSGiResult> myCloseReference =
					new AtomicReference<>();

				AtomicReference<OSGiResult> otherCloseReference =
					new AtomicReference<>();

				ConcurrentDoublyLinkedList<Tuple<T>> identities =
					new ConcurrentDoublyLinkedList<>();

				ConcurrentDoublyLinkedList<Tuple<Function<T, S>>> funs =
					new ConcurrentDoublyLinkedList<>();

				return new OSGiResultImpl(
					() -> {
						OSGiResultImpl or1 = _operation.run(
							bundleContext,
							t -> {
								Node node = identities.addLast(t);

								t.onTermination(node::remove);

								funs.forEach(f -> processAdded(op, f, t));
							}
						);

						myCloseReference.set(or1);

						OSGiResultImpl funRun =
							((OSGiImpl<Function<T, S>>) fun)._operation.run(
								bundleContext,
								f -> {
									Node node = funs.addLast(f);

									f.onTermination(node::remove);

									identities.forEach(
										t -> processAdded(op, f, t));
								});

						otherCloseReference.set(funRun);

						or1.start();

						funRun.start();
					},
					() -> {
						myCloseReference.get().close();

						otherCloseReference.get().close();
					});
			}
			));
	}

	private <S> void processAdded(
		Consumer<Tuple<S>> addedSource, Tuple<Function<T, S>> fTuple,
		Tuple<T> t) {

		Tuple<S> tuple = t.map(fTuple.getContent());

		fTuple.addRelatedTuple(tuple);

		addedSource.accept(tuple);
	}

}


