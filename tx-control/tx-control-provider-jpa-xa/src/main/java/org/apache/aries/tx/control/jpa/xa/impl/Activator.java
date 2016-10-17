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
package org.apache.aries.tx.control.jpa.xa.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.tx.control.jpa.common.impl.AbstractJPAEntityManagerProvider;
import org.apache.aries.tx.control.jpa.common.impl.InternalJPAEntityManagerProviderFactory;
import org.apache.aries.tx.control.jpa.common.impl.JPAResourceActivator;
import org.apache.aries.tx.control.jpa.common.impl.ResourceTrackingJPAEntityManagerProviderFactory;
import org.apache.aries.tx.control.resource.common.impl.ConfigurationDefinedResourceFactory;
import org.apache.aries.tx.control.resource.common.impl.ResourceProviderFactoryServiceFactory;
import org.apache.aries.tx.control.resource.common.impl.TrackingResourceProviderFactory;
import org.osgi.framework.BundleContext;

public class Activator extends JPAResourceActivator {

	@Override
	protected ResourceProviderFactoryServiceFactory<AbstractJPAEntityManagerProvider, ResourceTrackingJPAEntityManagerProviderFactory> getServiceFactory(
			BundleContext context) {
		
		InternalJPAEntityManagerProviderFactory ijempf = new JPAEntityManagerProviderFactoryImpl();
		return new ResourceProviderFactoryServiceFactory<AbstractJPAEntityManagerProvider, ResourceTrackingJPAEntityManagerProviderFactory>() {
			@Override
			protected TrackingResourceProviderFactory<AbstractJPAEntityManagerProvider> getTrackingResourceManagerProviderFactory() {
				return new ResourceTrackingJPAEntityManagerProviderFactory(ijempf);
			}
			
		};
	}

	@Override
	protected Dictionary<String, Object> getServiceProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("osgi.xa.enabled", Boolean.TRUE);
		return props;
	}

	@Override
	protected ConfigurationDefinedResourceFactory getConfigurationDefinedResourceFactory(BundleContext context) {
		return new XAJPAManagedServiceFactory(context);
	}

	@Override
	protected String getMSFPid() {
		return "org.apache.aries.tx.control.jpa.xa";
	}

}