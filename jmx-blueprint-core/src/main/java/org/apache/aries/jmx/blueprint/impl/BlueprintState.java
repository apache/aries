/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jmx.blueprint.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.aries.jmx.blueprint.BlueprintStateMBean;
import org.apache.aries.jmx.blueprint.codec.OSGiBlueprintEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

public class BlueprintState implements BlueprintStateMBean, MBeanRegistration {

    private BundleContext context;

    private ServiceRegistration listenerReg;

    private Map<Long, CompositeData> dataMap = new HashMap<Long, CompositeData>();

    public BlueprintState(BundleContext context) {
        this.context = context;
    }

    public synchronized long[] getBlueprintBundleIds() throws IOException {
        Long[] bundleIdKeys = dataMap.keySet().toArray(new Long[0]);
        long[] bundleIds = new long[bundleIdKeys.length];
        for (int i = 0; i < bundleIdKeys.length; i++) {
            bundleIds[i] = bundleIdKeys[i].longValue();
        }
        return bundleIds;
    }

    public synchronized CompositeData getLastEvent(long bundleId) throws IOException {
        return dataMap.get(Long.valueOf(bundleId));
    }

    public synchronized TabularData getLastEvents() throws IOException {
        TabularDataSupport table = new TabularDataSupport(BlueprintStateMBean.OSGI_BLUEPRINT_EVENTS_TYPE);
        table.putAll(dataMap);
        return table;
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        // no op
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        BlueprintListener listener = new BlueprintStateListener();
        // reg listener
        listenerReg = context.registerService(BlueprintListener.class.getName(), listener, null);
    }

    public void preDeregister() throws Exception {
        // deregister Listener
        try{
            listenerReg.unregister();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void postDeregister() {
        // no op
    }

    private class BlueprintStateListener implements BlueprintListener {
        public synchronized void blueprintEvent(BlueprintEvent event) {
            CompositeData data = new OSGiBlueprintEvent(event).asCompositeData();
            dataMap.put(Long.valueOf(event.getBundle().getBundleId()), data);
        }

    }

}
