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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author Carlos Sierra Andrés
 */
public class ConfigurationOSGiImpl
	extends OSGiImpl<Dictionary<String, ?>> {

	public ConfigurationOSGiImpl(String pid) {
		super(bundleContext -> {
			AtomicReference<Dictionary<String, ?>> atomicReference =
				new AtomicReference<>(null);

			AtomicReference<Tuple<Dictionary<String, ?>>>
				tupleAtomicReference = new AtomicReference<>(
				Tuple.create(null));

			AtomicReference<ServiceRegistration<ManagedService>>
				serviceRegistrationReferece = new AtomicReference<>(null);

			Pipe<Tuple<Dictionary<String, ?>>, Tuple<Dictionary<String, ?>>>
				added = Pipe.create();

			Consumer<Tuple<Dictionary<String, ?>>> addedSource =
				added.getSource();

			Pipe<Tuple<Dictionary<String, ?>>, Tuple<Dictionary<String, ?>>>
				removed = Pipe.create();

			Consumer<Tuple<Dictionary<String, ?>>> removedSource =
				removed.getSource();

			Runnable start = () ->
				serviceRegistrationReferece.set(
					bundleContext.registerService(
						ManagedService.class,
						properties -> {
							while (!atomicReference.compareAndSet(
								tupleAtomicReference.get().t,
								properties)) {
							}

							Tuple<Dictionary<String, ?>> old =
								tupleAtomicReference.get();

							if (old.t != null) {
								removedSource.accept(old);
							}

							Tuple<Dictionary<String, ?>> tuple =
								Tuple.create(properties);

							if (properties != null) {
								addedSource.accept(tuple);
							}

							tupleAtomicReference.set(tuple);
						},
						new Hashtable<String, Object>() {{
							put("service.pid", pid);
						}}));

			return new OSGiResultImpl<>(
				added, removed, start,
				() -> serviceRegistrationReferece.get().unregister());
		});
	}

}
