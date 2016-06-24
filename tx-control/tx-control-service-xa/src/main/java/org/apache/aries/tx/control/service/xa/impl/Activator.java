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
package org.apache.aries.tx.control.service.xa.impl;

import static org.apache.aries.tx.control.service.xa.impl.Activator.ChangeType.RECREATE;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.transaction.control.TransactionControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {
	
	private static final String PID = "org.apache.aries.tx.control.service.xa";

	/**
	 * This will be more useful once the OSGi converter exists, for now it just
	 * generates a metatype for this service.
	 */
	@ObjectClassDefinition(pid=PID, description="Apache Aries Transaction Control Service (XA)")
	@interface Config {
		@AttributeDefinition(name="Enable recovery", required=false, description="Enable recovery")
		boolean recovery_enabled() default false;

		@AttributeDefinition(name="Recovery Log storage folder", required=false, description="Transaction Recovery Log directory")
		boolean recovery_log_dir();
		
		@AttributeDefinition(name="Transaction Timeout", required=false, description="Transaction Timeout in seconds")
		int transaction_timeout() default 300;
	}
	
	private static final Logger logger = LoggerFactory.getLogger(Activator.class);

	private ServiceRegistration<ManagedService> msReg;
	
	private boolean open;

	private TransactionControlImpl txControlImpl;
	
	private ServiceRegistration<TransactionControl> txControlReg;
	
	private Map<String, Object> configuration;

	private BundleContext context;

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		
		synchronized (this) {
			open = true;
		}
		
		Dictionary<String, Object> properties = getMSProperties();
		logger.info("Registering for configuration updates {}", properties);
		ManagedService service = c -> configurationUpdated(c, false);
		msReg = context.registerService(ManagedService.class, service, properties);
		
		new Thread(() -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
			configurationUpdated(null, true);
		}).start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		safeUnregister(msReg);
		
		ServiceRegistration<TransactionControl> toUnregister;
		TransactionControlImpl toClose;
		synchronized (this) {
			open = false;
			toUnregister = txControlReg;
			txControlReg = null;
			toClose = txControlImpl;
			txControlImpl = null;
			configuration = null;
		}
		cleanUp(toUnregister, toClose);
	}

	
	private void configurationUpdated(Dictionary<String, ?> config, boolean internal) {
		Map<String,Object> newConfig = toMap(config);
		Runnable action;
		synchronized (this) {
			if(!open) {
				return;
			}
			
			if(internal && configuration != null) {
				// We can ignore the internal call as we've been configured;
				return;
			}
			
			ChangeType change = txControlImpl == null ? RECREATE :
					txControlImpl.changed(newConfig, txControlReg == null);
			switch(change) {
				case NONE :
					action = () -> {};
					break;
				case SERVICE_PROPS:
					ServiceRegistration<TransactionControl> toUpdate = txControlReg;
					TransactionControlImpl implToQuery = txControlImpl;
					action = () -> toUpdate.setProperties(implToQuery.getProperties());
					break;
				case RECREATE :
					ServiceRegistration<TransactionControl> toUnregister = txControlReg;
					TransactionControlImpl toClose = txControlImpl;
					txControlReg = null;
					txControlImpl = null;
					action = () -> {
						
							cleanUp(toUnregister, toClose);
						
							TransactionControlImpl impl = null;
							ServiceRegistration<TransactionControl> newReg = null;
							try {
								impl = new TransactionControlImpl(context, newConfig);
								newReg = context.registerService(TransactionControl.class, 
												impl, impl.getProperties());
							} catch (Exception e) {
								if(newReg != null) {
									safeUnregister(newReg);
								} 
								if (impl != null) {
									impl.destroy();
								}
							}
							boolean cleanUp = true;
							synchronized (Activator.this) {
								if(configuration == newConfig) {
									txControlImpl = impl;
									txControlReg = newReg;
									cleanUp = false;
								}
							}
							
							if(cleanUp) {
								cleanUp(newReg, impl);
							}
						};
					
					break;
				default :
					throw new IllegalArgumentException("An unknown change occurred " + change);
			}
			configuration = newConfig;
		}
		action.run();
	}

	private void cleanUp(ServiceRegistration<TransactionControl> toUnregister, 
			TransactionControlImpl toClose) {
		safeUnregister(toUnregister);
		if(toClose != null) {
			toClose.destroy();
		}
	}
	
	private void safeUnregister(ServiceRegistration<?> reg) {
		if(reg != null) {
			try {
				reg.unregister();
			} catch (IllegalStateException ise) {
				// A No Op
			}
		}
	}

	private Map<String, Object> toMap(Dictionary<String, ?> config) {
		Map<String, Object> configMap = new HashMap<>();
		
		if(config != null) {
			Enumeration<String> keys = config.keys();
			while(keys.hasMoreElements()) {
				String key = keys.nextElement();
				configMap.put(key, config.get(key));
			}
		}
		return configMap;
	}

	public static enum ChangeType {
		NONE, SERVICE_PROPS, RECREATE;
	}
	
	private Dictionary<String, Object> getMSProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(Constants.SERVICE_PID, PID);
		props.put(Constants.SERVICE_DESCRIPTION, "Managed Service for the Apache Aries Transaction Control Service with XA Transactions");
		props.put(Constants.SERVICE_VENDOR, "Apache Aries");
		return props;
	}
}
