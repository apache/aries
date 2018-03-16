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
import org.apache.aries.osgi.functional.Publisher;
import org.apache.aries.osgi.functional.Transformer;
import org.apache.aries.osgi.functional.internal.ConcurrentDoublyLinkedList.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Carlos Sierra Andrés
 */
public class OSGiImpl<T> implements OSGi<T> {

	public OSGiOperationImpl<T> _operation;

	public OSGiImpl(OSGiOperationImpl<T> operation) {
		_operation = operation;
	}

	@Override
	public <S> OSGi<S> applyTo(OSGi<Function<T, S>> fun) {
		return new OSGiImpl<>(
			(bundleContext, op) -> {
				AtomicReference<OSGiResult> myCloseReference =
					new AtomicReference<>();

				AtomicReference<OSGiResult> otherCloseReference =
					new AtomicReference<>();

				ConcurrentDoublyLinkedList<T> identities =
					new ConcurrentDoublyLinkedList<>();

				ConcurrentDoublyLinkedList<Function<T, S>> funs =
					new ConcurrentDoublyLinkedList<>();

				return new OSGiResultImpl(
					() -> {
						OSGiResultImpl or1 = _operation.run(
							bundleContext,
							t -> {
								Node node = identities.addLast(t);

								List<Runnable> terminators = funs.stream().map(
									f -> op.apply(f.apply(t))
								).collect(
									Collectors.toList()
								);

								return () -> {
									node.remove();

									terminators.forEach(Runnable::run);
								};
							}
						);

						myCloseReference.set(or1);

						OSGiResultImpl funRun =
							((OSGiImpl<Function<T, S>>) fun)._operation.run(
								bundleContext,
								f -> {
									Node node = funs.addLast(f);

									List<Runnable> terminators =
										identities.stream().map(
											t -> op.apply(f.apply(t))
										).collect(
											Collectors.toList()
										);

									return () -> {
										node.remove();

										terminators.forEach(Runnable::run);
									};
								});

						otherCloseReference.set(funRun);

						or1.start();

						funRun.start();
					},
					() -> {
						myCloseReference.get().close();

						otherCloseReference.get().close();
					});
			});
	}

	@Override
	public <S> OSGi<S> choose(
		Predicate<T> chooser, Function<OSGi<T>, OSGi<S>> then,
		Function<OSGi<T>, OSGi<S>> otherwise) {

		return new OSGiImpl<>((bundleContext, publisher) -> {
			Pad<T, S> thenPad = new Pad<>(bundleContext, then, publisher);
			Pad<T, S> elsePad = new Pad<>(bundleContext, otherwise, publisher);

			OSGiResultImpl result = _operation.run(
				bundleContext,
				t -> {
					if (chooser.test(t)) {
						return thenPad.publish(t);
					} else {
						return elsePad.publish(t);
					}
				});
			return new OSGiResultImpl(
				result::start,
				() -> {
					thenPad.close();
					elsePad.close();
					result.close();
				});
		});
	}

	@Override
	@SafeVarargs
	public final <S> OSGi<S> distribute(Function<OSGi<T>, OSGi<S>> ... funs) {
		return new OSGiImpl<>((bundleContext, publisher) -> {
			List<Pad<T, S>> pads =
				Arrays.stream(
					funs
				).map(
					fun -> new Pad<>(bundleContext, fun, publisher)
				).collect(
					Collectors.toList()
			);

			OSGiResultImpl result = _operation.run(
				bundleContext,
				t -> {
					List<Runnable> terminators =
						pads.stream().map(p -> p.publish(t)).collect(
							Collectors.toList());

					return () -> {
						terminators.forEach(Runnable::run);
					};
				});

			return new OSGiResultImpl(
				result::start,
				() -> {
					result.close();

					pads.forEach(Pad::close);
				});
		});
	}

	@Override
	public OSGi<T> effects(
		Consumer<? super T> onAdded, Consumer<? super T> onRemoved) {

		return new OSGiImpl<>((bundleContext, op) ->
			_operation.run(
				bundleContext,
				t -> {
					onAdded.accept(t);

					Runnable terminator;
					try {
						terminator = op.apply(t);
					}
					catch (Exception e) {
						onRemoved.accept(t);

						throw e;
					}

					return () -> {
						onRemoved.accept(t);

						terminator.run();
					};
				}));
	}

	@Override
	public OSGi<T> filter(Predicate<T> predicate) {
		return new OSGiImpl<>((bundleContext, op) ->
			_operation.run(
				bundleContext,
				(t) -> {
					if (predicate.test(t)) {
						return op.apply(t);
					}
					else {
						return () -> {};
					}
				}
			));
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
	public <S> OSGi<S> map(Function<? super T, ? extends S> function) {
		return new OSGiImpl<>((bundleContext, op) ->
			_operation.run(bundleContext, t -> op.apply(function.apply(t))));
	}

	@Override
	public OSGi<T> recover(BiFunction<T, Exception, T> onError) {
		return new OSGiImpl<>((bundleContext, op) ->
			_operation.run(
				bundleContext,
				t -> {
					try {
						return op.apply(t);
					}
					catch (Exception e) {
						return op.apply(onError.apply(t, e));
					}
				}
			));
	}

	@Override
	public OSGi<T> recoverWith(BiFunction<T, Exception, OSGi<T>> onError) {
		return new OSGiImpl<>((bundleContext, op) ->
			_operation.run(
				bundleContext,
				t -> {
					try {
						return op.apply(t);
					}
					catch (Exception e) {
						OSGi<T> errorProgram = onError.apply(t, e);

						OSGiResult result =
							((OSGiImpl<T>) errorProgram).run(bundleContext, op);

						return result::close;
					}
				}
			));
	}

	@Override
	public <K, S> OSGi<S> splitBy(
		Function<T, K> mapper, Function<OSGi<T>, OSGi<S>> fun) {

		return new OSGiImpl<>((bundleContext, op) -> {
			HashMap<K, Pad<T, S>> pads = new HashMap<>();

			OSGiResultImpl result = _operation.run(
				bundleContext,
				t ->
					pads.computeIfAbsent(
						mapper.apply(t),
						__ -> new Pad<>(bundleContext, fun, op)
					).apply(t)
			);

			return new OSGiResultImpl(
				result::start,
				() -> {
					pads.values().forEach(Pad::close);

					result.close();
				});
		});
	}

	@Override
	public <S> OSGi<S> then(OSGi<S> next) {
		return flatMap(ignored -> next);
	}

	@Override
	public <S> OSGi<S> transform(Transformer<T, S> fun) {
		return new TransformerOSGi<>(this, fun);
	}

	@Override
	public OSGiResult run(BundleContext bundleContext) {
		return run(bundleContext, x -> {});
	}

	@Override
	public OSGiResult run(BundleContext bundleContext, Consumer<T> andThen) {
		return run(bundleContext, t -> {andThen.accept(t); return NOOP;});
	}

	public OSGiResult run(BundleContext bundleContext, Publisher<T> op) {
		OSGiResultImpl result = _operation.run(bundleContext, op);

		result.start();

		return result;
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

}


