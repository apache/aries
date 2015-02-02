/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.transaction.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.transaction.internal.NLS;
import org.apache.aries.transaction.internal.TransactionManagerService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Activator implements BundleActivator, ManagedService {

    public static final String PID = "org.apache.aries.transaction";

    private static final Logger log = LoggerFactory.getLogger(PID);

    private BundleContext bundleContext;
    private TransactionManagerService manager;

    public void start(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        // Make sure TransactionManager comes up even if no config admin is installed
        updated(null);
        bundleContext.registerService(ManagedService.class.getName(), this, getProps());
    }

    private Dictionary<String, Object> getProps() {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, PID);
        return props;
    }

    public void stop(BundleContext context) throws Exception {
        deleted();
    }

    public synchronized void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
        if (properties == null) {
            properties = getProps();
        }
        deleted();
        manager = new TransactionManagerService(PID, properties, bundleContext);
        try {
            manager.start();
        } catch (Exception e) {
            log.error(NLS.MESSAGES.getMessage("exception.tx.manager.start"), e);
        }
    }

    public synchronized void deleted() {
        if (manager != null) {
            try {
                manager.close();
            } catch (Exception e) {
                log.error(NLS.MESSAGES.getMessage("exception.tx.manager.stop"), e);
            } finally {
                manager = null;
            }
        }
    }

}