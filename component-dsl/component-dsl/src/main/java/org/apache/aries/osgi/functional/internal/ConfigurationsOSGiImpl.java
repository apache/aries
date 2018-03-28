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

import org.apache.aries.osgi.functional.Publisher;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class ConfigurationsOSGiImpl
	extends OSGiImpl<Dictionary<String, ?>> {

	public ConfigurationsOSGiImpl(String factoryPid) {
		super((bundleContext, op) -> {
			Map<String, Runnable> results = new ConcurrentHashMap<>();

			AtomicBoolean closed = new AtomicBoolean();

			ServiceRegistration<ManagedServiceFactory> serviceRegistration =
				bundleContext.registerService(
					ManagedServiceFactory.class,
					new ConfigurationsManagedServiceFactory(
						results, op, closed),
					new Hashtable<String, Object>() {{
						put("service.pid", factoryPid);
					}});

			return new OSGiResultImpl(
				() -> {
					closed.set(true);

					serviceRegistration.unregister();

					results.values().forEach(Runnable::run);

					results.clear();
				});
		});
	}

	private static class ConfigurationsManagedServiceFactory
		implements ManagedServiceFactory {

		private final Map<String, Runnable> _results;

		private final Publisher<? super Dictionary<String, ?>> _op;
		private AtomicBoolean _closed;

		public ConfigurationsManagedServiceFactory(
			Map<String, Runnable> results,
			Publisher<? super Dictionary<String, ?>> op,
			AtomicBoolean closed) {

			_results = results;
			_op = op;
			_closed = closed;
		}

		@Override
		public void deleted(String s) {
			Runnable runnable = _results.remove(s);

			runnable.run();
		}

		@Override
		public String getName() {
			return "Functional OSGi Managed Service Factory";
		}

		@Override
		public void updated(String s, Dictionary<String, ?> dictionary)
			throws ConfigurationException {

			Runnable terminator = _op.apply(dictionary);

			Runnable old = _results.put(s, terminator);

			if (old != null) {
				old.run();
			}

			if (_closed.get()) {
				/* if we have been closed while executing the effects we have
				   to check if this terminator has been left unexecuted.
				*/
				_results.computeIfPresent(
					s,
					(key, runnable) -> {
						runnable.run();

					return null;
				});
			}
		}

	}
}
