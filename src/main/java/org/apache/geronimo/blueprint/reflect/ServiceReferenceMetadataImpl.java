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
package org.apache.geronimo.blueprint.reflect;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.osgi.service.blueprint.reflect.Listener;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;

/**
 * Implementation of ServiceReferenceMetadata 
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public abstract class ServiceReferenceMetadataImpl extends ComponentMetadataImpl implements ServiceReferenceMetadata {

    protected int availability;
    protected List<String> interfaceNames;
    protected String componentName;
    protected String filter;
    protected Collection<Listener> serviceListeners;

    public ServiceReferenceMetadataImpl() {
    }

    public ServiceReferenceMetadataImpl(ServiceReferenceMetadata source) {
        super(source);
        this.availability = source.getAvailability();
        this.interfaceNames = new ArrayList<String>(source.getInterfaceNames());
        this.componentName = source.getComponentName();
        this.filter = source.getFilter();
        for (Listener listener : source.getServiceListeners()) {
            addServiceListener(new ListenerImpl(listener));
        }
    }

    public int getAvailability() {
        return availability;
    }

    public void setAvailability(int availability) {
        this.availability = availability;
    }

    public List<String> getInterfaceNames() {
        return interfaceNames;
    }

    public void setInterfaceNames(List<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
    }

    public void addInterfaceName(String interfaceName) {
        if (this.interfaceNames == null) {
            this.interfaceNames = new ArrayList<String>();
        }
        this.interfaceNames.add(interfaceName);
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Collection<Listener> getServiceListeners() {
        if (this.serviceListeners == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableCollection(this.serviceListeners);
        }
    }

    public void setServiceListeners(Collection<Listener> listeners) {
        this.serviceListeners = listeners;
    }

    public void addServiceListener(Listener bindingListenerMetadata) {
        if (this.serviceListeners == null) {
            this.serviceListeners = new ArrayList<Listener>();
        }
        this.serviceListeners.add(bindingListenerMetadata);
    }

}
