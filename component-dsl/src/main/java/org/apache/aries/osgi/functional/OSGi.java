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


package org.apache.aries.osgi.functional;

import org.apache.aries.functional.Function2;
import org.apache.aries.functional.Function3;
import org.apache.aries.functional.Function4;
import org.apache.aries.functional.Function5;
import org.apache.aries.functional.Function6;
import org.apache.aries.osgi.functional.internal.BundleContextOSGiImpl;
import org.apache.aries.osgi.functional.internal.BundleOSGi;
import org.apache.aries.osgi.functional.internal.ChangeContextOSGiImpl;
import org.apache.aries.osgi.functional.internal.ConfigurationOSGiImpl;
import org.apache.aries.osgi.functional.internal.ConfigurationsOSGiImpl;
import org.apache.aries.osgi.functional.internal.DistributeOSGi;
import org.apache.aries.osgi.functional.internal.IgnoreImpl;
import org.apache.aries.osgi.functional.internal.JustOSGiImpl;
import org.apache.aries.osgi.functional.internal.NothingOSGiImpl;
import org.apache.aries.osgi.functional.internal.OnCloseOSGiImpl;
import org.apache.aries.osgi.functional.internal.ServiceReferenceOSGi;
import org.apache.aries.osgi.functional.internal.ServiceRegistrationOSGiImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceRegistration;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Carlos Sierra Andrés
 */
public interface OSGi<T> extends OSGiRunnable<T> {
	Runnable NOOP = () -> {};

	OSGi<T> effects(
		Consumer<? super T> onAdded, Consumer<? super T> onRemoved);

	static OSGi<Void> ignore(OSGi<?> program) {
		return new IgnoreImpl(program);
	}

	<S> OSGi<S> map(Function<? super T, ? extends S> function);

	<S> OSGi<S> flatMap(Function<? super T, OSGi<? extends S>> fun);

	<S> OSGi<S> then(OSGi<S> next);

	OSGi<Void> foreach(Consumer<? super T> onAdded);

	OSGi<Void> foreach(
		Consumer<? super T> onAdded, Consumer<? super T> onRemoved);

	static OSGi<BundleContext> bundleContext() {

		return new BundleContextOSGiImpl();
	}

	static OSGi<Bundle> bundles(int stateMask) {
		return new BundleOSGi(stateMask);
	}

	static <T> OSGi<T> changeContext(
		BundleContext bundleContext, OSGi<T> program) {

		return new ChangeContextOSGiImpl<>(program, bundleContext);
	}

	static OSGi<Dictionary<String, ?>> configuration(String pid) {
		return new ConfigurationOSGiImpl(pid);
	}

	static OSGi<Dictionary<String, ?>> configurations(String factoryPid) {
		return new ConfigurationsOSGiImpl(factoryPid);
	}

	static <S> OSGi<S> just(S s) {
		return new JustOSGiImpl<>(s);
	}

	static <S> OSGi<S> just(Collection<S> s) {
		return new JustOSGiImpl<>(s);
	}

	static <S> OSGi<S> just(Supplier<S> s) {
		return new JustOSGiImpl<>(() -> Collections.singletonList(s.get()));
	}

	static <S> OSGi<S> join(OSGi<OSGi<S>> program) {
		return program.flatMap(x -> x);
	}

	static <S> OSGi<S> nothing() {
		return new NothingOSGiImpl<>();
	}

	static OSGi<Void> onClose(Runnable action) {
		return new OnCloseOSGiImpl(action);
	}

	static OSGi<ServiceObjects<Object>> prototypes(String filterString) {
		return prototypes(null, filterString);
	}

	static <T> OSGi<ServiceObjects<T>> prototypes(Class<T> clazz) {
		return prototypes(clazz, null);
	}

	static <T> OSGi<ServiceObjects<T>> prototypes(
		Class<T> clazz, String filterString) {

		return
		bundleContext().flatMap(
		bundleContext ->

		serviceReferences(clazz, filterString).map(
			CachingServiceReference::getServiceReference
		).map(
			bundleContext::getServiceObjects)
		);
	}

	static <T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, T service, Map<String, Object> properties) {

		return new ServiceRegistrationOSGiImpl<>(clazz, service, properties);
	}

	static <T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, ServiceFactory<T> service, Map<String, Object> properties) {

		return new ServiceRegistrationOSGiImpl<>(clazz, service, properties);
	}

	static OSGi<ServiceRegistration<?>> register(
		String[] classes, Object service, Map<String, ?> properties) {

		return new ServiceRegistrationOSGiImpl(classes, service, properties);
	}

	static <T> OSGi<T> services(Class<T> clazz) {
		return services(clazz, null);
	}

	static <T> OSGi<Object> services(String filterString) {
		return services(null, filterString);
	}

	static <T> OSGi<T> services(Class<T> clazz, String filterString) {
		return
			bundleContext().flatMap(
			bundleContext ->

			serviceReferences(clazz, filterString).map(
				CachingServiceReference::getServiceReference
			).flatMap(
				sr -> {
					T service = bundleContext.getService(sr);

					return
						onClose(() -> bundleContext.ungetService(sr)).then(
						just(service)
					);
			}
		));
	}

	public static <T> OSGi<T> once(OSGi<T> program) {
		return program.route(router -> {
			AtomicInteger count = new AtomicInteger();

			AtomicReference<SentEvent> terminator = new AtomicReference<>();

			router.onIncoming(t -> {
				int c = count.getAndIncrement();

				if (c == 0) {
					terminator.set(router.signalAdd(t));
				}
			});

			router.onLeaving(t -> {
				int c = count.decrementAndGet();

				if (c == 0) {
					SentEvent s = terminator.getAndSet(null);

					s.terminate();
				}
			});
		});
	}

	static <T> OSGi<CachingServiceReference<T>> serviceReferences(
		Class<T> clazz) {

		return new ServiceReferenceOSGi<>(null, clazz);
	}

	static OSGi<CachingServiceReference<Object>> serviceReferences(
		String filterString) {

		return new ServiceReferenceOSGi<>(filterString, null);
	}

	static <T> OSGi<CachingServiceReference<T>> serviceReferences(
		Class<T> clazz, String filterString) {

		return new ServiceReferenceOSGi<>(filterString, clazz);
	}

	static <T> OSGi<CachingServiceReference<T>> serviceReferences(
		Class<T> clazz, String filterString,
		Refresher<? super CachingServiceReference<T>> onModified) {

		return new ServiceReferenceOSGi<>(filterString, clazz, onModified);
	}

	static <T> OSGi<CachingServiceReference<T>> serviceReferences(
		Class<T> clazz, Refresher<? super CachingServiceReference<T>> onModified) {

		return new ServiceReferenceOSGi<>(null, clazz, onModified);
	}

	static OSGi<CachingServiceReference<Object>> serviceReferences(
		String filterString,
		Refresher<? super CachingServiceReference<Object>> onModified) {

		return new ServiceReferenceOSGi<>(filterString, null, onModified);
	}



	@SafeVarargs
	static <T> OSGi<T> all(OSGi<T> ... programs) {
		return new DistributeOSGi<>(programs);
	}

	OSGi<T> filter(Predicate<T> predicate);

	OSGi<T> route(Consumer<Router<T>> routerConsumer);

	interface Router<T> {

		void onIncoming(Consumer<Event<T>> adding);
		void onLeaving(Consumer<Event<T>> removing);

		void onStart(Runnable start);
		void onClose(Runnable close);

		SentEvent<T> signalAdd(Event<T> event);
	}

	public default <S> OSGi<S> applyTo(OSGi<Function<T, S>> fun) {
		return fun.flatMap(this::map);
	}

	public static <A, B, C> OSGi<C> combine(Function2<A, B, C> fun, OSGi<A> a, OSGi<B> b) {
		return b.applyTo(a.applyTo(just(fun.curried())));
	}

	public static <A, B, C, D> OSGi<D> combine(Function3<A, B, C, D> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c) {
		return c.applyTo(OSGi.combine((A aa, B bb) -> fun.curried().apply(aa).apply(bb), a, b));
	}

	public static <A, B, C, D, E> OSGi<E> combine(Function4<A, B, C, D, E> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d) {
		return d.applyTo(OSGi.combine((A aa, B bb, C cc) -> fun.curried().apply(aa).apply(bb).apply(cc), a, b, c));
	}

	public static <A, B, C, D, E, F> OSGi<F> combine(Function5<A, B, C, D, E, F> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e) {
		return e.applyTo(OSGi.combine((A aa, B bb, C cc, D dd) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd), a, b, c, d));
	}

	public static <A, B, C, D, E, F, G> OSGi<G> combine(Function6<A, B, C, D, E, F, G> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f) {
		return f.applyTo(OSGi.combine((A aa, B bb, C cc, D dd, E ee) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee), a, b, c, d, e));
	}

}
