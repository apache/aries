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
import org.apache.aries.osgi.functional.internal.BundleMOSGi;
import org.apache.aries.osgi.functional.internal.ChangeContextOSGiImpl;
import org.apache.aries.osgi.functional.internal.ConfigurationOSGiImpl;
import org.apache.aries.osgi.functional.internal.ConfigurationsOSGiImpl;
import org.apache.aries.osgi.functional.internal.JustOSGiImpl;
import org.apache.aries.osgi.functional.internal.NothingOSGiImpl;
import org.apache.aries.osgi.functional.internal.OnCloseOSGiImpl;
import org.apache.aries.osgi.functional.internal.PrototypesMOSGi;
import org.apache.aries.osgi.functional.internal.ServiceReferenceOSGi;
import org.apache.aries.osgi.functional.internal.ServiceRegistrationOSGiImpl;
import org.apache.aries.osgi.functional.internal.ServicesMOSGi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public interface OSGi<T> extends OSGiRunnable<T> {
	Runnable NOOP = () -> {};

	<S> OSGi<S> map(Function<T, S> function);

	<S> OSGi<S> flatMap(Function<T, OSGi<S>> fun);

	<S> OSGi<S> then(OSGi<S> next);

	<S> OSGi<Void> foreach(Function<T, OSGi<S>> fun);

	static OSGi<BundleContext> bundleContext() {

		return new BundleContextOSGiImpl();
	}

	static MOSGi<Bundle> bundles(int stateMask) {
		return new BundleMOSGi(stateMask);
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

	static <S> OSGi<S> nothing() {
		return new NothingOSGiImpl<>();
	}

	static OSGi<Void> onClose(Runnable action) {
		return new OnCloseOSGiImpl(action);
	}

	static <S> OSGi<S> just(S s) {
		return new JustOSGiImpl<>(s);
	}

	static <T> MOSGi<ServiceObjects<T>> prototypes(Class<T> clazz) {
		return prototypes(clazz, null);
	}

	static <T> MOSGi<ServiceObjects<T>> prototypes(
		Class<T> clazz, String filterString) {

		return new PrototypesMOSGi<>(clazz, filterString);
	}

	static <T, S extends T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, S service, Map<String, Object> properties) {

		return new ServiceRegistrationOSGiImpl<>(
			clazz, service, properties);
	}

	static <T> MOSGi<T> services(Class<T> clazz) {
		return services(clazz, null);
	}

	static <T> MOSGi<T> services(Class<T> clazz, String filterString) {
		return new ServicesMOSGi<>(clazz, filterString);
	}

	static <T> OSGi<ServiceReference<T>> serviceReferences(
		Class<T> clazz, String filterString) {

		return new ServiceReferenceOSGi<>(filterString, clazz);
	}

}
