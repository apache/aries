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
package org.apache.aries.tx.control.service.local.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.transaction.control.TransactionControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);
	private TransactionControlImpl service;
	private ServiceRegistration<TransactionControl> reg;
	
	@Override
	public void start(BundleContext context) throws Exception {
		Dictionary<String, Object> properties = getProperties();
		logger.info("Registering a new Local TransactionControl service with properties {}", properties);
		service = new TransactionControlImpl();
		reg = context.registerService(TransactionControl.class, 
				service, properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if(reg != null) {
			try {
				reg.unregister();
			} catch (IllegalStateException ise) { }
		}
		service.close();
	}

	private Dictionary<String, Object> getProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("osgi.local.enabled", Boolean.TRUE);
		props.put(Constants.SERVICE_DESCRIPTION, "The Apache Aries Transaction Control Service for Local Transactions");
		props.put(Constants.SERVICE_VENDOR, "Apache Aries");
		return props;
	}
}
