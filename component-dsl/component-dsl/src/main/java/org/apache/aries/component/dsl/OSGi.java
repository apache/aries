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


package org.apache.aries.component.dsl;

import org.apache.aries.component.dsl.function.Function10;
import org.apache.aries.component.dsl.function.Function14;
import org.apache.aries.component.dsl.function.Function16;
import org.apache.aries.component.dsl.function.Function19;
import org.apache.aries.component.dsl.function.Function2;
import org.apache.aries.component.dsl.function.Function20;
import org.apache.aries.component.dsl.function.Function25;
import org.apache.aries.component.dsl.function.Function4;
import org.apache.aries.component.dsl.function.Function6;
import org.apache.aries.component.dsl.function.Function8;
import org.apache.aries.component.dsl.function.Function9;
import org.apache.aries.component.dsl.internal.CoalesceOSGiImpl;
import org.apache.aries.component.dsl.internal.ConfigurationOSGiImpl;
import org.apache.aries.component.dsl.internal.EffectsOSGi;
import org.apache.aries.component.dsl.internal.NothingOSGiImpl;
import org.apache.aries.component.dsl.internal.Pad;
import org.apache.aries.component.dsl.internal.ServiceReferenceOSGi;
import org.apache.aries.component.dsl.internal.ServiceRegistrationOSGiImpl;
import org.apache.aries.component.dsl.function.Function11;
import org.apache.aries.component.dsl.function.Function12;
import org.apache.aries.component.dsl.function.Function13;
import org.apache.aries.component.dsl.function.Function15;
import org.apache.aries.component.dsl.function.Function17;
import org.apache.aries.component.dsl.function.Function18;
import org.apache.aries.component.dsl.function.Function21;
import org.apache.aries.component.dsl.function.Function22;
import org.apache.aries.component.dsl.function.Function23;
import org.apache.aries.component.dsl.function.Function24;
import org.apache.aries.component.dsl.function.Function26;
import org.apache.aries.component.dsl.function.Function3;
import org.apache.aries.component.dsl.function.Function5;
import org.apache.aries.component.dsl.function.Function7;
import org.apache.aries.component.dsl.internal.BundleContextOSGiImpl;
import org.apache.aries.component.dsl.internal.BundleOSGi;
import org.apache.aries.component.dsl.internal.ChangeContextOSGiImpl;
import org.apache.aries.component.dsl.internal.ConcurrentDoublyLinkedList;
import org.apache.aries.component.dsl.internal.ConfigurationsOSGiImpl;
import org.apache.aries.component.dsl.internal.AllOSGi;
import org.apache.aries.component.dsl.internal.IgnoreImpl;
import org.apache.aries.component.dsl.internal.JustOSGiImpl;
import org.apache.aries.component.dsl.internal.OSGiImpl;
import org.apache.aries.component.dsl.internal.UpdateSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Carlos Sierra Andrés
 */
public interface OSGi<T> extends OSGiRunnable<T> {
	OSGiResult NOOP = () -> {};

	@SafeVarargs
	static <T> OSGi<T> all(OSGi<T> ... programs) {
		return new AllOSGi<>(programs);
	}

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

	@SafeVarargs
	static <T> OSGi<T> coalesce(OSGi<T> ... programs) {
		return new CoalesceOSGiImpl<>(programs);
	}

	static <A, B, RES> OSGi<RES> combine(Function2<A, B, RES> fun, OSGi<A> a, OSGi<B> b) {
		return b.applyTo(a.applyTo(just(fun.curried())));
	}

	static <A, B, C, RES> OSGi<RES> combine(Function3<A, B, C, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c) {
		return c.applyTo(combine((A aa, B bb) -> fun.curried().apply(aa).apply(bb), a, b));
	}

	static <A, B, C, D, RES> OSGi<RES> combine(Function4<A, B, C, D, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d) {
		return d.applyTo(combine((A aa, B bb, C cc) -> fun.curried().apply(aa).apply(bb).apply(cc), a, b, c));
	}

	static <A, B, C, D, E, RES> OSGi<RES> combine(Function5<A, B, C, D, E, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e) {
		return e.applyTo(combine((A aa, B bb, C cc, D dd) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd), a, b, c, d));
	}

	static <A, B, C, D, E, F, RES> OSGi<RES> combine(Function6<A, B, C, D, E, F, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f) {
		return f.applyTo(combine((A aa, B bb, C cc, D dd, E ee) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee), a, b, c, d, e));
	}

	static <A, B, C, D, E, F, G, RES> OSGi<RES> combine(Function7<A, B, C, D, E, F, G, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g) {
		return g.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff), a, b, c, d, e, f));
	}

	static <A, B, C, D, E, F, G, H, RES> OSGi<RES> combine(Function8<A, B, C, D, E, F, G, H, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h) {
		return h.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg), a, b, c, d, e, f, g));
	}

	static <A, B, C, D, E, F, G, H, I, RES> OSGi<RES> combine(Function9<A, B, C, D, E, F, G, H, I, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i) {
		return i.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh), a, b, c, d, e, f, g, h));
	}

	static <A, B, C, D, E, F, G, H, I, J, RES> OSGi<RES> combine(Function10<A, B, C, D, E, F, G, H, I, J, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j) {
		return j.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii), a, b, c, d, e, f, g, h, i));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, RES> OSGi<RES> combine(Function11<A, B, C, D, E, F, G, H, I, J, K, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k) {
		return k.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj), a, b, c, d, e, f, g, h, i, j));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, RES> OSGi<RES> combine(Function12<A, B, C, D, E, F, G, H, I, J, K, L, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l) {
		return l.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk), a, b, c, d, e, f, g, h, i, j, k));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, RES> OSGi<RES> combine(Function13<A, B, C, D, E, F, G, H, I, J, K, L, M, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m) {
		return m.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll), a, b, c, d, e, f, g, h, i, j, k, l));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, RES> OSGi<RES> combine(Function14<A, B, C, D, E, F, G, H, I, J, K, L, M, N, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n) {
		return n.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm), a, b, c, d, e, f, g, h, i, j, k, l, m));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, RES> OSGi<RES> combine(Function15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o) {
		return o.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn), a, b, c, d, e, f, g, h, i, j, k, l, m, n));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, RES> OSGi<RES> combine(Function16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p) {
		return p.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, RES> OSGi<RES> combine(Function17<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q) {
		return q.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, RES> OSGi<RES> combine(Function18<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r) {
		return r.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, RES> OSGi<RES> combine(Function19<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s) {
		return s.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, RES> OSGi<RES> combine(Function20<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t) {
		return t.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, RES> OSGi<RES> combine(Function21<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u) {
		return u.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, RES> OSGi<RES> combine(Function22<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v) {
		return v.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt, U uu) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt).apply(uu), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, RES> OSGi<RES> combine(Function23<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w) {
		return w.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt, U uu, V vv) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt).apply(uu).apply(vv), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, RES> OSGi<RES> combine(Function24<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w, OSGi<X> x) {
		return x.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt, U uu, V vv, W ww) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt).apply(uu).apply(vv).apply(ww), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, RES> OSGi<RES> combine(Function25<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w, OSGi<X> x, OSGi<Y> y) {
		return y.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt, U uu, V vv, W ww, X xx) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt).apply(uu).apply(vv).apply(ww).apply(xx), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, RES> OSGi<RES> combine(Function26<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w, OSGi<X> x, OSGi<Y> y, OSGi<Z> z) {
		return z.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt, U uu, V vv, W ww, X xx, Y yy) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt).apply(uu).apply(vv).apply(ww).apply(xx).apply(yy), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y));
	}

	static OSGi<Dictionary<String, ?>> configuration(String pid) {
		return new ConfigurationOSGiImpl(pid);
	}

	static OSGi<Dictionary<String, ?>> configurations(String factoryPid) {
		return new ConfigurationsOSGiImpl(factoryPid);
	}

	static OSGi<Void> effect(Effect<Void> effect) {
		return new EffectsOSGi(
			() -> effect.getOnIncoming().accept(null),
			() -> effect.getOnLeaving().accept(null));
	}

	static OSGi<Void> effects(Runnable onAdding, Runnable onRemoving) {
		return new EffectsOSGi(onAdding, onRemoving);
	}

	static <T> OSGi<T> fromOsgiRunnable(OSGiRunnable<T> runnable) {
		return getOsgiFactory().create(runnable);
	}

	static OSGiFactory getOsgiFactory() {
		return OSGiImpl::create;
	}

	static OSGi<Void> ignore(OSGi<?> program) {
		return new IgnoreImpl(program);
	}

	static <S> OSGi<S> join(OSGi<OSGi<S>> program) {
		return program.flatMap(x -> x);
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

	static <S> OSGi<S> nothing() {
		return new NothingOSGiImpl<>();
	}

	@Deprecated()
	/**
	 * @deprecated see {@link #effects(Runnable, Runnable)}
	 */
	static OSGi<Void> onClose(Runnable action) {
		return effects(NOOP, action);
	}

	static <T> OSGi<T> once(OSGi<T> program) {
		return program.transform(op -> {
			AtomicInteger count = new AtomicInteger();

			AtomicReference<Runnable> terminator = new AtomicReference<>();

			return t -> {
				if (count.getAndIncrement() == 0) {
					terminator.set(op.apply(t));
				}

				return () -> UpdateSupport.defer(() -> {
					if (count.decrementAndGet() == 0) {
						Runnable runnable = terminator.getAndSet(NOOP);

						runnable.run();
					}
				});
			};
		});
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

	static <T> OSGi<ServiceObjects<T>> prototypes(
		CachingServiceReference<T> serviceReference) {

		return
			bundleContext().flatMap(bundleContext ->
			just(bundleContext.getServiceObjects(serviceReference.getServiceReference())));
	}

	static <T> OSGi<ServiceObjects<T>> prototypes(
		ServiceReference<T> serviceReference) {

		return
			bundleContext().flatMap(bundleContext ->
			just(bundleContext.getServiceObjects(serviceReference)));
	}

	static <T> OSGi<ServiceObjects<T>> prototypes(
		OSGi<ServiceReference<T>> serviceReference) {

		return serviceReference.flatMap(OSGi::prototypes);
	}

	static <T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, T service, Map<String, Object> properties) {

		return register(clazz, () -> service, () -> properties);
	}

	static <T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, ServiceFactory<T> service,
		Map<String, Object> properties) {

		return register(clazz, service, () -> properties);
	}

	static OSGi<ServiceRegistration<?>> register(
		String[] classes, Object service, Map<String, ?> properties) {

		return new ServiceRegistrationOSGiImpl(
			classes, () -> service, () -> properties);
	}

	static <T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, Supplier<T> service, Supplier<Map<String, ?>> properties) {

		return new ServiceRegistrationOSGiImpl<>(clazz, service, properties);
	}

	static <T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, ServiceFactory<T> service,
		Supplier<Map<String, ?>> properties) {

		return new ServiceRegistrationOSGiImpl<>(clazz, service, properties);
	}

	static OSGi<ServiceRegistration<?>> register(
		String[] classes, Supplier<Object> service,
		Supplier<Map<String, ?>> properties) {

		return new ServiceRegistrationOSGiImpl(classes, service, properties);
	}

	static <T> OSGi<T> service(ServiceReference<T> serviceReference) {
		return
			bundleContext().flatMap(bundleContext -> {
				T service = bundleContext.getService(serviceReference);

				return
					onClose(() -> bundleContext.ungetService(serviceReference)).
						then(
					just(service));
			});
	}

	static <T> OSGi<T> service(CachingServiceReference<T> serviceReference) {
		return
			bundleContext().flatMap(bundleContext -> {
				T service = bundleContext.getService(
					serviceReference.getServiceReference());

				return
					onClose(() -> bundleContext.ungetService(
						serviceReference.getServiceReference())).
						then(
							just(service));
			});
	}

	static <T> OSGi<T> service(
		OSGi<CachingServiceReference<T>> serviceReference) {

		return serviceReference.flatMap(OSGi::service);
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

	default  <S> OSGi<S> applyTo(OSGi<Function<T, S>> fun) {
		return fromOsgiRunnable((bundleContext, op) -> {
			ConcurrentDoublyLinkedList<T> identities =
				new ConcurrentDoublyLinkedList<>();

			ConcurrentDoublyLinkedList<Function<T, S>> funs =
				new ConcurrentDoublyLinkedList<>();

			OSGiResult myResult = run(
				bundleContext,
				t -> {
					ConcurrentDoublyLinkedList.Node node =
						identities.addLast(t);

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

			OSGiResult funRun = fun.run(
				bundleContext,
				f -> {
					ConcurrentDoublyLinkedList.Node node = funs.addLast(f);

					List<Runnable> terminators = identities.stream().map(
						t -> op.apply(f.apply(t))
					).collect(
						Collectors.toList()
					);

					return () -> {
						node.remove();

						terminators.forEach(Runnable::run);
					};
				});

			return
				() -> {
					myResult.close();

					funRun.close();
				};
		});
	}

	default <S> OSGi<S> choose(
		Function<T, OSGi<Boolean>> chooser, Function<OSGi<T>, OSGi<S>> then,
		Function<OSGi<T>, OSGi<S>> otherwise) {

		return fromOsgiRunnable((bundleContext, publisher) -> {
			Pad<T, S> thenPad = new Pad<>(bundleContext, then, publisher);
			Pad<T, S> elsePad = new Pad<>(bundleContext, otherwise, publisher);

			OSGiResult result = run(
				bundleContext,
				t -> chooser.apply(t).run(
                    bundleContext,
                    b -> {
                        if (b) {
                            return thenPad.publish(t);
                        } else {
                            return elsePad.publish(t);
                        }
                    }
                ));
			return () -> {
				thenPad.close();
				elsePad.close();
				result.close();
			};
		});
	}

	default <S> OSGi<S> distribute(Function<OSGi<T>, OSGi<S>> ... funs) {
		return fromOsgiRunnable((bundleContext, publisher) -> {
			List<Pad<T, S>> pads =
				Arrays.stream(
					funs
				).map(
					fun -> new Pad<>(bundleContext, fun, publisher)
				).collect(
					Collectors.toList()
				);

			OSGiResult result = run(
				bundleContext,
				t -> {
					List<Runnable> terminators =
						pads.stream().map(p -> p.publish(t)).collect(
							Collectors.toList());

					return () -> terminators.forEach(Runnable::run);
				});

			return () -> {
				result.close();

				pads.forEach(Pad::close);
			};
		});
	}

	default OSGi<T> effects(
		Consumer<? super T> onAdded, Consumer<? super T> onRemoved) {

		return fromOsgiRunnable((bundleContext, op) ->
			run(
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

	default OSGi<T> effects(Effect<? super T> effect) {
		return effects(effect.getOnIncoming(), effect.getOnLeaving());
	}

	default OSGi<T> filter(Predicate<T> predicate) {
		return fromOsgiRunnable((bundleContext, op) ->
			run(
				bundleContext,
				t -> {
					if (predicate.test(t)) {
						return op.apply(t);
					}
					else {
						return NOOP;
					}
				}
			));
	}

	default <S> OSGi<S> flatMap(Function<? super T, OSGi<? extends S>> fun) {
		return fromOsgiRunnable((bundleContext, op) ->
			run(bundleContext, t -> fun.apply(t).run(bundleContext, op))
		);
	}

	default OSGi<Void> foreach(Consumer<? super T> onAdded) {
		return foreach(onAdded, __ -> {});
	}

	default OSGi<Void> foreach(
		Consumer<? super T> onAdded, Consumer<? super T> onRemoved) {

		return ignore(effects(onAdded, onRemoved));
	}

	default <S> OSGi<S> map(Function<? super T, ? extends S> function) {
		return fromOsgiRunnable((bundleContext, op) ->
			run(bundleContext, t -> op.apply(function.apply(t)))
		);
	}

	default OSGi<T> recover(BiFunction<T, Exception, T> onError) {
		return fromOsgiRunnable((bundleContext, op) ->
			run(
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

	default OSGi<T> recoverWith(BiFunction<T, Exception, OSGi<T>> onError) {
		return fromOsgiRunnable((bundleContext, op) ->
			run(
				bundleContext,
				t -> {
					try {
						return op.apply(t);
					}
					catch (Exception e) {
						return onError.apply(t, e).run(bundleContext, op);
					}
				}
			));
	}

	default <K, S> OSGi<S> splitBy(
		Function<T, OSGi<K>> mapper, BiFunction<K, OSGi<T>, OSGi<S>> fun) {

		return fromOsgiRunnable((bundleContext, op) -> {
			HashMap<K, Pad<T, S>> pads = new HashMap<>();

			OSGiResult result = run(
				bundleContext,
				t -> mapper.apply(t).run(
					bundleContext,
					k -> pads.computeIfAbsent(
						k,
						__ -> new Pad<>(
							bundleContext,
							___ -> fun.apply(k, ___), op)
					).publish(t)
				)
			);

			return () -> {
				pads.values().forEach(Pad::close);

				result.close();
			};
		});
	}

	default public <S> OSGi<S> then(OSGi<S> next) {
		return flatMap(__ -> next);
	}

	default <S> OSGi<S> transform(Transformer<T, S> fun) {
		return fromOsgiRunnable(
			(bundleContext, op) -> run(bundleContext, fun.transform(op)));
	}

}
