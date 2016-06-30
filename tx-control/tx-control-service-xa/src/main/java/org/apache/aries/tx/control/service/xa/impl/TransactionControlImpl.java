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
import static org.apache.aries.tx.control.service.xa.impl.Activator.ChangeType.SERVICE_PROPS;
import static org.apache.aries.tx.control.service.xa.impl.LocalResourceSupport.ENFORCE_SINGLE;

import java.io.File;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.resource.spi.IllegalStateException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.service.common.impl.AbstractTransactionContextImpl;
import org.apache.aries.tx.control.service.common.impl.AbstractTransactionControlImpl;
import org.apache.aries.tx.control.service.xa.impl.Activator.ChangeType;
import org.apache.geronimo.transaction.log.HOWLLog;
import org.apache.geronimo.transaction.manager.NamedXAResource;
import org.apache.geronimo.transaction.manager.NamedXAResourceFactory;
import org.apache.geronimo.transaction.manager.RecoveryWorkAroundTransactionManager;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.apache.geronimo.transaction.manager.XidFactoryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.transaction.control.recovery.RecoverableXAResource;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionControlImpl extends AbstractTransactionControlImpl {

	private static final Logger logger = LoggerFactory.getLogger(TransactionControlImpl.class);
	
	private Map<String, Object> config;
	private final XidFactory xidFactory;
	private final HOWLLog log;
	private final RecoveryWorkAroundTransactionManager transactionManager;
	private final LocalResourceSupport localResourceSupport;
	private final ServiceTracker<RecoverableXAResource, RecoverableXAResource> recoverableResources;

	public TransactionControlImpl(BundleContext ctx, Map<String, Object> config) throws Exception {
		
		try {
			this.config = config;
			this.localResourceSupport = getLocalResourceSupport();
			xidFactory = new XidFactoryImpl();
			log = getLog(ctx);
			
			if(log != null) {
				log.doStart();
			}
			
			transactionManager = new RecoveryWorkAroundTransactionManager(getTimeout(),
					xidFactory, log);
			
			if(log != null) {
				recoverableResources = 
						new ServiceTracker<RecoverableXAResource, RecoverableXAResource>(
								ctx, RecoverableXAResource.class, null) {

									@Override
									public RecoverableXAResource addingService(
											ServiceReference<RecoverableXAResource> reference) {
										RecoverableXAResource resource = super.addingService(reference);
										
										if(resource.getId() == null) {
											logger.warn("The RecoverableXAResource service with id {} does not have a name and will be ignored", 
													reference.getProperty("service.id"));
											return null;
										}
										
										if(log == null) {
											logger.warn("A RecoverableXAResource with id {} has been registered, but recovery logging is disabled for this Transaction Control service. No recovery will be availble in the event of a Transaction Manager failure.", resource.getId());
										}
										
										transactionManager.registerNamedXAResourceFactory(new NamedXAResourceFactory() {
											
											@Override
											public void returnNamedXAResource(NamedXAResource namedXAResource) {
												resource.releaseXAResource(((NamedXAResourceImpl)namedXAResource).xaResource);
											}
											
											@Override
											public NamedXAResource getNamedXAResource() throws SystemException {
												try {
													XAResource xaResource = resource.getXAResource();
													if(xaResource == null) {
														throw new IllegalStateException("The recoverable resource " + resource.getId() 
														+ " is currently unavailable");
													}
													return new NamedXAResourceImpl(resource.getId(), xaResource,
															transactionManager, false);
												} catch (Exception e) {
													throw new SystemException("Unable to get recoverable resource " + 
															resource.getId() + ": " + e.getMessage());
												}
											}
											
											@Override
											public String getName() {
												return resource.getId();
											}
										});
										
										return resource;
									}

									@Override
									public void removedService(ServiceReference<RecoverableXAResource> reference,
											RecoverableXAResource service) {
										transactionManager.unregisterNamedXAResourceFactory(service.getId());
									}
					
								};
				recoverableResources.open();
			} else {
				recoverableResources = null;
			}
		} catch (Exception e) {
			destroy();
			throw e;
		}
	}

	private LocalResourceSupport getLocalResourceSupport() {
		Object o = config.getOrDefault("local.resources", ENFORCE_SINGLE);
		return o instanceof LocalResourceSupport ? (LocalResourceSupport) o : 
			LocalResourceSupport.valueOf(o.toString());
	}

	private HOWLLog getLog(BundleContext ctx) throws Exception {
		Object recovery = config.getOrDefault("recovery.log.enabled", false);
		
		if (recovery instanceof Boolean ? (Boolean) recovery : Boolean.valueOf(recovery.toString())) {
			String logFileExt = "log";
            String logFileName = "transaction";
            
            String logFileDir;

            Object o = config.get("recovery.log.dir");
            if(o == null) {
            	logFileDir = ctx.getDataFile("recoveryLog").getAbsolutePath();
            } else {
            	logFileDir = o.toString();
            }
            
            File f = new File(logFileDir);
            if(f.exists() && !f.isDirectory()) {
            	throw new IllegalArgumentException("The recovery log directory " + logFileDir + 
            			" is not a directory.");
            }
            
            HOWLLog log = new HOWLLog("org.objectweb.howl.log.BlockLogBuffer",
                                             4,
                                             true,
                                             true,
                                             50,
                                             logFileDir,
                                             logFileExt,
                                             logFileName,
                                             -1,
                                             0,
                                             2,
                                             4,
                                             -1,
                                             true,
                                             xidFactory,
                                             null);
			return log;
		}
		// null means a non-recoverable log
		return null;
	}
	
	private int getTimeout() {
		Object o = config.getOrDefault("transaction.timeout", 300);
		return o instanceof Integer ? (Integer) o : Integer.valueOf(o.toString());
	}
	
	public void destroy() {
		if(recoverableResources != null) {
			recoverableResources.close();
		}
		if(log != null) {
			try {
				log.doStop();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public synchronized Dictionary<String, ?> getProperties() {
		Dictionary<String, Object> props = new Hashtable<>();

		config.entrySet().stream()
			.filter(e -> !e.getKey().startsWith("."))
			.forEach(e -> props.put(e.getKey(), e.getValue()));
		
		props.put("osgi.xa.enabled", Boolean.TRUE);
		props.put("osgi.local.enabled", Boolean.TRUE);
		props.put(Constants.SERVICE_DESCRIPTION, "The Apache Aries Transaction Control Service for XA Transactions");
		props.put(Constants.SERVICE_VENDOR, "Apache Aries");
		
		return props;
	}
	
	/**
	 * This method can be used to define config changes that should not trigger
	 * the service to be unregistered and recreated
	 * 
	 * @param original
	 * @param isRegistered
	 * @return
	 */
	public synchronized ChangeType changed(Map<String, Object> updated, boolean isRegistered) {
		Map<String, Object> current = filterFixedProps(updated);
		Map<String, Object> replacement = filterFixedProps(updated);
		
		// If our configuration is unchanged then just issue a service property update
		if(current.equals(replacement)) {
			config = updated; 
			return SERVICE_PROPS;
		}
		
		return RECREATE;
	}
	
	private Map<String, Object> filterFixedProps(Map<String, Object> raw) {
		Map<String, Object> filtered = new HashMap<>();
		
		copy(raw, filtered, "transaction.timeout");
		copy(raw, filtered, "recovery.log.enabled");
		copy(raw, filtered, "recovery.log.dir");
		copy(raw, filtered, "local.resources");
		
		return filtered;
	}

	private void copy(Map<String, Object> raw, Map<String, Object> filtered, String key) {
		if(raw.containsKey(key)) {
			filtered.put(key, raw.get(key));
		}
	}
	
	@Override
	protected AbstractTransactionContextImpl startTransaction(boolean readOnly) {
		return new TransactionContextImpl(transactionManager, readOnly, localResourceSupport);
	}
	
}
