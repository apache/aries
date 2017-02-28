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

import org.apache.aries.osgi.functional.internal.BundleContextOSGiImpl;
import org.apache.aries.osgi.functional.internal.BundleOSGi;
import org.apache.aries.osgi.functional.internal.ChangeContextOSGiImpl;
import org.apache.aries.osgi.functional.internal.ConfigurationOSGiImpl;
import org.apache.aries.osgi.functional.internal.ConfigurationsOSGiImpl;
import org.apache.aries.osgi.functional.internal.JustOSGiImpl;
import org.apache.aries.osgi.functional.internal.NothingOSGiImpl;
import org.apache.aries.osgi.functional.internal.OnCloseOSGiImpl;
import org.apache.aries.osgi.functional.internal.PrototypesOSGi;
import org.apache.aries.osgi.functional.internal.ServiceReferenceOSGi;
import org.apache.aries.osgi.functional.internal.ServiceRegistrationOSGiImpl;
import org.apache.aries.osgi.functional.internal.ServicesOSGi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Carlos Sierra Andrés
 */
public interface OSGi<T> extends OSGiRunnable<T> {
	Runnable NOOP = () -> {};

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

		return new PrototypesOSGi<>(clazz, filterString);
	}

	static <T, S extends T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, S service, Map<String, Object> properties) {

		return new ServiceRegistrationOSGiImpl<>(
			clazz, service, properties);
	}

	static <T> OSGi<T> services(Class<T> clazz) {
		return services(clazz, null);
	}

	static <T> OSGi<Object> services(String filterString) {
		return services(null, filterString);
	}

	static <T> OSGi<T> services(Class<T> clazz, String filterString) {
		return new ServicesOSGi<>(clazz, filterString);
	}

	static <T> OSGi<ServiceReference<T>> serviceReferences(
		Class<T> clazz) {

		return new ServiceReferenceOSGi<>(null, clazz);
	}

	static OSGi<ServiceReference<Object>> serviceReferences(
		String filterString) {

		return new ServiceReferenceOSGi<>(filterString, null);
	}

	static <T> OSGi<ServiceReference<T>> serviceReferences(
		Class<T> clazz, String filterString) {

		return new ServiceReferenceOSGi<>(filterString, clazz);
	}

	OSGi<T> filter(Predicate<T> predicate);

	OSGi<Void> distribute(Function<T, OSGi<?>>... funs);
}
