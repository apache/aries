/**
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
package org.apache.geronimo.blueprint.compendium.cm;

import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: if we need to make those exported services tied to their references as for other <service/> elements
 * TODO: it becomes a problem as currently we would have to create a specific recipe or something like that
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766508 $, $Date: 2009-04-19 22:09:27 +0200 (Sun, 19 Apr 2009) $
 */
public class CmManagedServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmManagedServiceFactory.class);

    private String id;
    private BlueprintContainer blueprintContainer;
    private ConfigurationAdmin configAdmin;
    private String factoryPid;
    private List<String> interfaces;
    private int autoExport;
    private int ranking;
    private String managedComponentName;

    private ServiceRegistration registration;
    private Map<String, ServiceRegistration> pids = new ConcurrentHashMap<String, ServiceRegistration>();
    private Map<ServiceRegistration, Object> services = new ConcurrentHashMap<ServiceRegistration, Object>();

    public void init() {
        LOGGER.debug("Initializing CmManagedServiceFactory for pid={}", factoryPid);
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, factoryPid);
        Bundle bundle = blueprintContainer.getBundleContext().getBundle();
        props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        props.put(Constants.BUNDLE_VERSION, bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        registration = blueprintContainer.getBundleContext().registerService(ManagedServiceFactory.class.getName(), new ConfigurationWatcher(), props);
    }

    public void destroy() {
        if (registration != null) {
            registration.unregister();
        }
    }

    public Map<ServiceRegistration, Object> getServiceMap() {
        return Collections.unmodifiableMap(services);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void setFactoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    public void setAutoExport(int autoExport) {
        this.autoExport = autoExport;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public void setManagedComponentName(String managedComponentName) {
        this.managedComponentName = managedComponentName;
    }

    protected void updated(String pid, Dictionary props) {
        LOGGER.error("Updated configuration {} with props {}", pid, props);
        Object component = blueprintContainer.getComponentInstance(managedComponentName);
        // TODO: autoExport, ranking, init instance, call listeners, etc...
        ServiceRegistration reg = blueprintContainer.getBundleContext().registerService(interfaces.toArray(new String[interfaces.size()]), component, new Properties());
        services.put(reg, component);
        pids.put(pid, reg);
    }

    protected void deleted(String pid) {
        LOGGER.error("Deleted configuration {}", pid);
        ServiceRegistration reg = pids.remove(pid);
        if (reg != null) {
            // TODO: destroy instance, etc...
            services.remove(reg);
            reg.unregister();
        }
    }

    private class ConfigurationWatcher implements ManagedServiceFactory {

        public String getName() {
            return null;
        }

        public void updated(String pid, Dictionary props) throws ConfigurationException {
            CmManagedServiceFactory.this.updated(pid, props);
        }

        public void deleted(String pid) {
            CmManagedServiceFactory.this.deleted(pid);
        }
    }

}
