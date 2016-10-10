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
package org.apache.aries.tx.control.jpa.common.impl;

import org.apache.aries.tx.control.resource.common.impl.ResourceActivator;
import org.apache.geronimo.specs.jpa.PersistenceActivator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JPAResourceActivator extends
	ResourceActivator<AbstractJPAEntityManagerProvider, ResourceTrackingJPAEntityManagerProviderFactory>{

	private static final Logger LOG = LoggerFactory.getLogger(JPAResourceActivator.class);
	
	private final BundleActivator geronimoActivator = new PersistenceActivator();
	
	@Override
	public void start(BundleContext context) throws Exception {
		LOG.debug("Starting JPA API trackers");
		geronimoActivator.start(context);
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			super.stop(context);
		} finally {
			LOG.debug("Stopping JPA API trackers");
			geronimoActivator.stop(context);
		}
	}


	@Override
	protected Class<JPAEntityManagerProviderFactory> getAdvertisedInterface() {
		return JPAEntityManagerProviderFactory.class;
	}
}
