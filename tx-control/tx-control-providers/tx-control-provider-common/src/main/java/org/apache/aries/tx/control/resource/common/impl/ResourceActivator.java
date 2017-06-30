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

import static org.osgi.framework.Constants.SERVICE_PID;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ResourceActivator<R extends AutoCloseable, F extends TrackingResourceProviderFactory<R>> implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(ResourceActivator.class);
	
	private ResourceProviderFactoryServiceFactory<R, F> service;
	private ConfigurationDefinedResourceFactory msf;
	
	private ServiceRegistration<?> reg;
	private ServiceRegistration<ManagedServiceFactory> factoryReg;
	
	@Override
	public void start(BundleContext context) throws Exception {
		
		service = getServiceFactory(context);
		
		if(service != null) {
			reg = context.registerService(getAdvertisedInterface().getName(), 
					service, getServiceProperties());
		}
		
		msf = getConfigurationDefinedResourceFactory(context);
		
		if(msf != null) {
			factoryReg = context.registerService(ManagedServiceFactory.class, 
					msf, getMSFProperties());
		}
		
		if(service == null && msf == null) {
			LOG.warn("The Resource Activator class {} defined no service factory or configuration defined resources", getClass().getName());
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		safeUnregister(reg);
		safeUnregister(factoryReg);
		if(msf != null) {
			try {
				msf.stop();
			} catch (Exception e) {
				LOG.error("There was an error closing the Configuration Defined Resource Manager", e);
			}
		}
		if(service != null) {
			try {
				service.close();
			} catch (Exception e) {
				LOG.error("There was an error closing the ResourceProviderFactory", e);
			}
		}
	}

	private void safeUnregister(ServiceRegistration<?> reg) {
		if(reg != null) {
			try {
				reg.unregister();
			} catch (IllegalStateException ise) {
				// Ignore this
			}
		}
	}

	protected Dictionary<String, ?> getMSFProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(SERVICE_PID, getMSFPid());
		return props;
	}

	protected ResourceProviderFactoryServiceFactory<R, F> getServiceFactory(BundleContext context) {
		return null;
	}

	protected Class<? super F> getAdvertisedInterface() {
		throw new UnsupportedOperationException("Resource factories are not supported");
	}
	
	protected Dictionary<String, Object> getServiceProperties() {
		throw new UnsupportedOperationException("Resource factories are not supported");
	}

	protected ConfigurationDefinedResourceFactory getConfigurationDefinedResourceFactory(
			BundleContext context) {
		return null;
	}
	
	protected String getMSFPid() {
		throw new UnsupportedOperationException("Configuration defined resources are not supported");
	}
}
