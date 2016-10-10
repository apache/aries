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
import java.util.Properties;

import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.aries.tx.control.jpa.common.impl.AbstractJPAManagedServiceFactory;
import org.apache.aries.tx.control.jpa.common.impl.AbstractManagedJPADataSourceSetup;
import org.apache.aries.tx.control.jpa.common.impl.AbstractManagedJPAEMFLocator;
import org.apache.aries.tx.control.resource.common.impl.LifecycleAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;

public class LocalJPAManagedServiceFactory extends AbstractJPAManagedServiceFactory {

	public LocalJPAManagedServiceFactory(BundleContext context) {
		super(context);
	}

	@Override
	public String getName() {
		return "Aries JPAEntityManagerProvider (Local only) service";
	}

	@Override
	protected AbstractManagedJPADataSourceSetup dataSourceTracking(BundleContext context, String pid, Map<String, Object> properties,
			Properties jdbcProps, Map<String, Object> jpaProps) throws InvalidSyntaxException, ConfigurationException {
		return new LocalJPADataSourceSetup(context, pid, jdbcProps, jpaProps, properties);
	}

	@Override
	protected AbstractManagedJPAEMFLocator emfTracking(BundleContext context, String pid, Map<String, Object> properties,
			Map<String, Object> jpaProps) throws InvalidSyntaxException, ConfigurationException {
		return new LocalJPAEMFLocator(context, pid, jpaProps, properties, null);
	}

	@Override
	protected PersistenceUnitTransactionType getTransactionType() {
		return PersistenceUnitTransactionType.RESOURCE_LOCAL;
	}
}
