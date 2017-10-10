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

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author Carlos Sierra Andrés
 */
public class ConfigurationsOSGiImpl
	extends OSGiImpl<Dictionary<String, ?>> {

	public ConfigurationsOSGiImpl(String factoryPid) {
		super(bundleContext -> {
			Map<String, Tuple<Dictionary<String, ?>>> results =
				new ConcurrentHashMap<>();

			AtomicReference<ServiceRegistration<ManagedServiceFactory>>
				serviceRegistrationReference = new AtomicReference<>(null);

			Pipe<Dictionary<String, ?>, Dictionary<String, ?>>
				added = Pipe.create();

			Consumer<Tuple<Dictionary<String, ?>>> addedSource =
				added.getSource();

			Runnable start = () ->
				serviceRegistrationReference.set(
					bundleContext.registerService(
						ManagedServiceFactory.class,
						new ConfigurationsManagedServiceFactory(
							results, addedSource),
						new Hashtable<String, Object>() {{
							put("service.pid", factoryPid);
						}}));


			return new OSGiResultImpl<>(
				added, start,
				() -> {
					serviceRegistrationReference.get().unregister();

					for (Tuple<Dictionary<String, ?>> tuple :
						results.values()) {

						tuple.terminate();
					}
				});
		});
	}

	private static class ConfigurationsManagedServiceFactory
		implements ManagedServiceFactory {

		private final Map<String, Tuple<Dictionary<String, ?>>> _results;

		private final Consumer<Tuple<Dictionary<String, ?>>> _addedSource;

		public ConfigurationsManagedServiceFactory(
			Map<String, Tuple<Dictionary<String, ?>>> results,
			Consumer<Tuple<Dictionary<String, ?>>> addedSource) {

			_results = results;
			_addedSource = addedSource;
		}

		@Override
		public void deleted(String s) {
			Tuple<Dictionary<String, ?>> tuple = _results.remove(s);

			tuple.terminate();
		}

		@Override
		public String getName() {
			return "Functional OSGi Managed Service Factory";
		}

		@Override
		public void updated(String s, Dictionary<String, ?> dictionary)
			throws ConfigurationException {

			Tuple<Dictionary<String, ?>> tuple = Tuple.create(dictionary);

			Tuple<Dictionary<String, ?>> old = _results.put(s, tuple);

			if (old != null) {
				old.terminate();
			}

			_addedSource.accept(tuple);
		}

	}
}
