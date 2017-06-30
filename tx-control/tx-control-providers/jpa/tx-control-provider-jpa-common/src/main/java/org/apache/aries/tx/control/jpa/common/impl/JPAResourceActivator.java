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
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JPAResourceActivator extends
	ResourceActivator<AbstractJPAEntityManagerProvider, ResourceTrackingJPAEntityManagerProviderFactory>{

	private static final Logger LOG = LoggerFactory.getLogger(JPAResourceActivator.class);
	
	@Override
	protected Class<JPAEntityManagerProviderFactory> getAdvertisedInterface() {
		return JPAEntityManagerProviderFactory.class;
	}
}
