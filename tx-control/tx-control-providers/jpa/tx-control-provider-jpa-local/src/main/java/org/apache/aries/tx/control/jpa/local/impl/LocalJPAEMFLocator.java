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
package org.apache.aries.tx.control.jpa.local.impl;

import java.util.Map;

import org.apache.aries.tx.control.jpa.common.impl.AbstractJPAEntityManagerProvider;
import org.apache.aries.tx.control.jpa.common.impl.AbstractManagedJPAEMFLocator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

public class LocalJPAEMFLocator extends AbstractManagedJPAEMFLocator {

	public LocalJPAEMFLocator(BundleContext context, String pid, Map<String, Object> jpaProperties,
			Map<String, Object> providerProperties, Runnable onClose) throws InvalidSyntaxException, ConfigurationException {
		super(context, pid, jpaProperties, providerProperties, onClose);
	}

	@Override
	protected AbstractJPAEntityManagerProvider getResourceProvider(BundleContext context,
			EntityManagerFactoryBuilder service, ServiceReference<EntityManagerFactoryBuilder> reference,
			Map<String, Object> jpaProperties, Map<String, Object> providerProperties, Runnable onClose) {
		return new JPAEntityManagerProviderFactoryImpl().getProviderFor(service,
				jpaProperties, providerProperties, onClose);
	}
}