/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.tx.control.resource.common.impl;

import static java.util.Optional.ofNullable;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfigurationDefinedResourceFactory implements ManagedServiceFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ConfigurationDefinedResourceFactory.class);
	
	private final Map<String, LifecycleAware> managedInstances = new ConcurrentHashMap<>();

	private final BundleContext context;

	public ConfigurationDefinedResourceFactory(BundleContext context) {
		this.context = context;
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {

		Map<String, Object> propsMap = new HashMap<>();

		Enumeration<String> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			propsMap.put(key, properties.get(key));
		}

		try {
			LifecycleAware existing = managedInstances.get(pid);
			
			LifecycleAware cdr;
			if(existing != null) {
				if(existing.update(propsMap)) {
					LOG.debug("The Configuration driven resource with pid {} updated successfully", pid);
					return;
				}
				closeCDR(pid, existing);
				
				cdr = getConfigurationDrivenResource(context, pid, propsMap);
				if(!managedInstances.replace(pid, existing, cdr)) {
					// We lost this race
					return;
				}
			} else {
				cdr = getConfigurationDrivenResource(context, pid, propsMap);
				if(managedInstances.putIfAbsent(pid, cdr) != null) {
					// We lost this race
					return;
				}
			}
			
			cdr.start();
		} catch (Exception e) {
			LOG.error("The configuration driven resource for pid {} encountered a failure", pid, e);
			
			if(e instanceof ConfigurationException) {
				throw (ConfigurationException) e;
			} else {
				throw new ConfigurationException(null, "A failure occured configuring the resource for pid " + pid, e);
			}
		}
	}

	protected abstract LifecycleAware getConfigurationDrivenResource(BundleContext context, 
			String pid, Map<String, Object> properties) throws Exception;

	public void stop() {
		managedInstances.entrySet().forEach(e -> closeCDR(e.getKey(), e.getValue()));
	}
	
	private void closeCDR(String pid, LifecycleAware cdr) {
		try {
			cdr.stop();
		} catch (Exception ex) {
			LOG.warn("There was an error stopping Configuration Driven Resource {}", pid, ex);
		}
	}

	@Override
	public void deleted(String pid) {
		ofNullable(managedInstances.remove(pid))
			.ifPresent(cdr -> closeCDR(pid, cdr));
	}
}
