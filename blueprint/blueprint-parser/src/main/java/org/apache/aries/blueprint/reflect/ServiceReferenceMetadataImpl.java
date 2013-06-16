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
package org.apache.aries.blueprint.reflect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.aries.blueprint.mutable.MutableServiceReferenceMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * Implementation of ServiceReferenceMetadata 
 *
 * @version $Rev$, $Date$
 */
public abstract class ServiceReferenceMetadataImpl extends ComponentMetadataImpl implements MutableServiceReferenceMetadata {

    protected int availability;
    protected String interfaceName;
    protected String componentName;
    protected String filter;
    protected Collection<ReferenceListener> referenceListeners;
    protected int proxyMethod;
    protected Class runtimeInterface;
    protected BundleContext bundleContext;
    protected ValueMetadata extendedFilter;

    public ServiceReferenceMetadataImpl() {
    }

    public ServiceReferenceMetadataImpl(ServiceReferenceMetadata source) {
        super(source);
        this.availability = source.getAvailability();
        this.interfaceName = source.getInterface();
        this.componentName = source.getComponentName();
        this.filter = source.getFilter();
        for (ReferenceListener listener : source.getReferenceListeners()) {
            addServiceListener(new ReferenceListenerImpl(listener));
        }
    }

    public int getAvailability() {
        return availability;
    }

    public void setAvailability(int availability) {
        this.availability = availability;
    }

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
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

    public Collection<ReferenceListener> getReferenceListeners() {
        if (this.referenceListeners == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableCollection(this.referenceListeners);
        }
    }

    public void setReferenceListeners(Collection<ReferenceListener> listeners) {
        this.referenceListeners = listeners != null ? new ArrayList<ReferenceListener>(listeners) : null;
    }

    public void addServiceListener(ReferenceListener bindingListenerMetadata) {
        if (this.referenceListeners == null) {
            this.referenceListeners = new ArrayList<ReferenceListener>();
        }
        this.referenceListeners.add(bindingListenerMetadata);
    }

    public ReferenceListener addServiceListener(Target listenerComponent, String bindMethodName, String unbindMethodName) {
        ReferenceListener listener = new ReferenceListenerImpl(listenerComponent, bindMethodName, unbindMethodName);
        addServiceListener(listener);
        return listener;
    }

    public void removeReferenceListener(ReferenceListener listener) {
        if (this.referenceListeners != null) {
            this.referenceListeners.remove(listener);
        }
    }

    public int getProxyMethod() {
        return proxyMethod;
    }

    public void setProxyMethod(int proxyMethod) {
        this.proxyMethod = proxyMethod;
    }

    public Class getRuntimeInterface() {
        return runtimeInterface;
    }

    public void setRuntimeInterface(Class runtimeInterface) {
        this.runtimeInterface = runtimeInterface;
    }
    
    public BundleContext getBundleContext() {
      return bundleContext;
    }
    
    public void setBundleContext(BundleContext ctx) {
      this.bundleContext = ctx;
    }

    public ValueMetadata getExtendedFilter() {
        return extendedFilter;
    }

    public void setExtendedFilter(ValueMetadata extendedFilter) {
        this.extendedFilter = extendedFilter;
    }
}
