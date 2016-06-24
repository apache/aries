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

import java.io.File;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.aries.tx.control.service.common.impl.AbstractTransactionContextImpl;
import org.apache.aries.tx.control.service.common.impl.AbstractTransactionControlImpl;
import org.apache.aries.tx.control.service.xa.impl.Activator.ChangeType;
import org.apache.geronimo.transaction.log.HOWLLog;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.apache.geronimo.transaction.manager.XidFactoryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class TransactionControlImpl extends AbstractTransactionControlImpl {

	private Map<String, Object> config;
	private final XidFactory xidFactory;
	private final HOWLLog log;
	private final GeronimoTransactionManager transactionManager;

	public TransactionControlImpl(BundleContext ctx, Map<String, Object> config) throws Exception {
		
		try {
			this.config = config;
			xidFactory = new XidFactoryImpl();
			log = getLog(ctx);
			
			if(log != null) {
				log.doStart();
			}
			
			transactionManager = new GeronimoTransactionManager(getTimeout(config),
					xidFactory, log);
		} catch (Exception e) {
			destroy();
			throw e;
		}
	}

	private HOWLLog getLog(BundleContext ctx) throws Exception {
		Object recovery = config.getOrDefault("recovery.enabled", false);
		
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
	
	private int getTimeout(Map<String, Object> newConfig) {
		Object o = newConfig.getOrDefault("transaction.timeout", 300);
		return o instanceof Integer ? (Integer) o : Integer.valueOf(o.toString());
	}
	
	public void destroy() {
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
		copy(raw, filtered, "recovery.enabled");
		copy(raw, filtered, "recovery.log.dir");
		
		return filtered;
	}

	private void copy(Map<String, Object> raw, Map<String, Object> filtered, String key) {
		if(raw.containsKey(key)) {
			filtered.put(key, raw.get(key));
		}
	}
	
	@Override
	protected AbstractTransactionContextImpl startTransaction(boolean readOnly) {
		return new TransactionContextImpl(transactionManager, readOnly);
	}
	
}
