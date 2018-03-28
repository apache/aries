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
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Carlos Sierra Andrés
 */
public class ConfigurationOSGiImpl extends OSGiImpl<Dictionary<String, ?>> {

	public ConfigurationOSGiImpl(String pid) {
		super((bundleContext, op) -> {
			AtomicReference<Dictionary<String, ?>> atomicReference =
				new AtomicReference<>(null);

			AtomicReference<Runnable>
				terminatorAtomicReference = new AtomicReference<>(() -> {});

			AtomicBoolean closed = new AtomicBoolean();


			ServiceRegistration<ManagedService> serviceRegistration =
				bundleContext.registerService(
					ManagedService.class,
					properties -> {
						atomicReference.set(properties);

						signalLeave(terminatorAtomicReference);

						if (properties != null) {
							terminatorAtomicReference.set(
								op.apply(properties));

							if (closed.get()) {
								/*
								if we have closed while executing the
								effects we have to execute the terminator
								directly instead of storing it
								*/
								signalLeave(terminatorAtomicReference);

								return;
							}
						}
					},
					new Hashtable<String, Object>() {{
						put("service.pid", pid);
					}});

			return new OSGiResultImpl(
				() -> {
					closed.set(true);

					serviceRegistration.unregister();

					signalLeave(terminatorAtomicReference);
				});
		});
	}

	private static void signalLeave(
		AtomicReference<Runnable> terminatorAtomicReference) {

		Runnable old = terminatorAtomicReference.getAndSet(null);

		if (old != null) {
            old.run();
        }
	}

}
