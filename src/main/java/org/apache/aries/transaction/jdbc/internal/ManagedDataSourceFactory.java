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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.transaction.jdbc.internal;

import org.apache.aries.transaction.AriesTransactionManager;
import org.apache.aries.transaction.jdbc.RecoverableDataSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import java.util.Hashtable;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ManagedDataSourceFactory {

    private final ServiceReference reference;
    private final AriesTransactionManager transactionManager;
    private final CommonDataSource dataSource;
    private final Map<String, Object> properties;

    private ServiceRegistration<DataSource> registration;
    private RecoverableDataSource ds;

    public ManagedDataSourceFactory(ServiceReference reference,
                                    AriesTransactionManager transactionManager) {
        this.reference = reference;
        this.transactionManager = transactionManager;
        this.properties = new Hashtable<String, Object>();
        for (String key : reference.getPropertyKeys()) {
            this.properties.put(key, reference.getProperty(key));
        }
        this.dataSource = (CommonDataSource) reference.getBundle().getBundleContext().getService(reference);
    }

    public AriesTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public CommonDataSource getDataSource() {
        return dataSource;
    }

    public String getResourceName() {
        return getString("aries.xa.name", null);
    }

    private String getString(String name, String def) {
        Object v = properties.get(name);
        if (v instanceof String) {
            return (String) v;
        } else {
            return def;
        }
    }

    private int getInt(String name, int def) {
        Object v = properties.get(name);
        if (v instanceof Integer) {
            return (Integer) v;
        } else if (v instanceof String) {
            return Integer.parseInt((String) v);
        } else {
            return def;
        }
    }

    private boolean getBool(String name, boolean def) {
        Object v = properties.get(name);
        if (v instanceof Boolean) {
            return (Boolean) v;
        } else if (v instanceof String) {
            return Boolean.parseBoolean((String) v);
        } else {
            return def;
        }
    }

    public void register() throws Exception {
        boolean isXaDataSource = (dataSource instanceof XADataSource);

        Hashtable<String, Object> props = new Hashtable<String, Object>(this.properties);
        props.put("aries.managed", "true");
        if (isXaDataSource) {
            props.put("aries.xa.aware", "true");
        }
        props.put("jmx.objectname", "org.apache.aries.transaction:type=jdbc,name=" + getResourceName());
        props.put(Constants.SERVICE_RANKING, getInt(Constants.SERVICE_RANKING, 0) + 1000);

        ds = new RecoverableDataSource();
        ds.setDataSource(dataSource);
        ds.setExceptionSorter(getString("aries.xa.exceptionSorter", "all"));
        ds.setUsername(getString("aries.xa.username", null));
        ds.setPassword(getString("aries.xa.password", null));

        ds.setTransactionManager(transactionManager);
        ds.setAllConnectionsEquals(getBool("aries.xa.allConnectionsEquals", true));
        ds.setConnectionMaxIdleMinutes(getInt("aries.xa.connectionMadIdleMinutes", 15));
        ds.setConnectionMaxWaitMilliseconds(getInt("aries.xa.connectionMaxWaitMilliseconds", 5000));
        ds.setPartitionStrategy(getString("aries.xa.partitionStrategy", null));
        ds.setPooling(getBool("aries.xa.pooling", true));
        ds.setPoolMaxSize(getInt("aries.xa.poolMaxSize", 10));
        ds.setPoolMinSize(getInt("aries.xa.poolMinSize", 0));
        ds.setValidateOnMatch(getBool("aries.xa.validateOnMatch", true));
        ds.setBackgroundValidation(getBool("aries.xa.backgroundValidation", false));
        ds.setBackgroundValidationMilliseconds(getInt("aries.xa.backgroundValidationMilliseconds", 600000));
        ds.setTransaction(getString("aries.xa.transaction", isXaDataSource ? "xa" : "local"));
        ds.setName(getResourceName());
        ds.start();

        BundleContext context = reference.getBundle().getBundleContext();
        registration = context.registerService(DataSource.class, ds, props);
    }

    public void unregister() throws Exception {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
        if (ds != null) {
            ds.stop();
        }
    }

}
