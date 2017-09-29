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

import org.apache.aries.osgi.functional.Event;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

		return new OSGiImpl<>(((bundleContext) -> {
			OSGiResultImpl<T> osgiResult = _operation.run(bundleContext);

			return new OSGiResultImpl<>(
				osgiResult.added.map(
					t -> {
						t.onTermination(() -> onRemoved.accept(t.t));

						return t.map(o -> {onAdded.accept(o); return null;});
					}),
				osgiResult.start, osgiResult.close);
		}));
	}

	@Override
	public <S> OSGi<S> map(Function<? super T, ? extends S> function) {
		return new OSGiImpl<>(((bundleContext) -> {
			OSGiResultImpl<T> osgiResult = _operation.run(bundleContext);

			return new OSGiResultImpl<>(
				osgiResult.added.map(t -> t.map(function)),
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
			osgiResult.added, osgiResult.start, osgiResult.close);
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

	private static class Pair<X, Y> {
		private final X _first;
		private final Y _second;

		public Pair(X first, Y second) {
			_first = first;
			_second = second;
		}

		public X getFirst() {
			return _first;
		}

		public Y getSecond() {
			return _second;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Pair<?, ?> pair = (Pair<?, ?>) o;

			return _first.equals(pair._first);
		}

		@Override
		public int hashCode() {
			return _first.hashCode();
		}
	}

	@Override
	public <S> OSGi<S> applyTo(OSGi<Function<T, S>> fun) {
		return new OSGiImpl<>(
			((bundleContext) -> {
				Map<Tuple<T>, List<Pair<Function<T, S>, Tuple<S>>>> identities =
					new HashMap<>();

				Map<Function<T, S>, List<Pair<Tuple<T>, Tuple<S>>>> funs =
					new IdentityHashMap<>();

				AtomicReference<OSGiResult<?>> myCloseReference =
					new AtomicReference<>();

				AtomicReference<OSGiResult<?>> otherCloseReference =
					new AtomicReference<>();

				Pipe<Tuple<S>, Tuple<S>> added = Pipe.create();

				Consumer<Tuple<S>> addedSource = added.getSource();

				OSGiResultImpl<S> osgiResult = new OSGiResultImpl<>(
					added,
					() -> {
						OSGiResultImpl<T> or1 = _operation.run(bundleContext);

						myCloseReference.set(or1);

						or1.added.map(t -> {
							synchronized (identities) {
								identities.put(t, new ArrayList<>());

								funs.keySet().forEach(f ->
									processAdded(
										identities, funs, addedSource, f, t));

								t.onTermination(() -> {
									synchronized (identities) {
										List<Pair<Function<T, S>, Tuple<S>>> remove =
											identities.remove(t);

										if (remove == null) {
											return;
										}

										remove.forEach(p -> {
											List<Pair<Tuple<T>, Tuple<S>>> pairs = funs.get(
												p.getFirst());

											if (pairs == null) {
												return;
											}

											pairs.remove(new Pair<>(t, null));

											p.getSecond().terminate();
										});
									}
								});

								return Tuple.create(null);
							}

						});

						OSGiResult<Void> or2 = fun.foreach(
							f -> {
								synchronized (identities) {
									funs.put(f, new ArrayList<>());

									identities.keySet().forEach(
										t -> processAdded(
											identities, funs, addedSource, f, t));
								}
							},
							f -> {
								synchronized (identities) {
									List<Pair<Tuple<T>, Tuple<S>>> remove = funs.remove(f);

									if (remove == null) {
										return;
									}

									remove.forEach(p -> {
										List<Pair<Function<T, S>, Tuple<S>>> pairs =
											identities.get(p.getFirst());

										Iterator<Pair<Function<T, S>, Tuple<S>>> iterator =
											pairs.iterator();

										while (iterator.hasNext()) {
											Pair<Function<T, S>, Tuple<S>> next = iterator.next();

											if (next.getFirst() == f) {
												iterator.remove();

												break;
											}
										}

										p.getSecond().terminate();
									});
								}
							}).run(bundleContext);

						or1.start.run();

						otherCloseReference.set(or2);
					},
					() -> {
						synchronized (identities) {
							identities.values().forEach(i ->
								i.forEach(
									p -> p.getSecond().terminate()));

							funs.clear();
						}

						myCloseReference.get().close();

						otherCloseReference.get().close();
					});

				return osgiResult;
			}
			));
	}

	private <S> void processAdded(
		Map<Tuple<T>, List<Pair<Function<T, S>, Tuple<S>>>> identities,
		Map<Function<T, S>, List<Pair<Tuple<T>, Tuple<S>>>> funs,
		Consumer<Tuple<S>> addedSource, Function<T, S> f, Tuple<T> t) {

		S result = f.apply(t.t);

		Tuple<S> tuple = Tuple.create(result);

		List<Pair<Function<T, S>, Tuple<S>>> tuples = identities.get(t);

		tuples.add(new Pair<>(f, tuple));

		List<Pair<Tuple<T>, Tuple<S>>> tuples2 = funs.get(f);

		tuples2.add(new Pair<>(t, tuple));

		addedSource.accept(tuple);
	}

}


