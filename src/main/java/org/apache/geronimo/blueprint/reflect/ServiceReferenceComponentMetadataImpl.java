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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.ArrayList;

import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ServiceReferenceComponentMetadataImpl extends ComponentMetadataImpl implements ServiceReferenceComponentMetadata {

    private int serviceAvailabilitySpecification;
    private Set<String> interfaceNames;
    private String componentName;
    private String filter;
    private Collection<BindingListenerMetadata> bindingListeners;

    public ServiceReferenceComponentMetadataImpl() {
        bindingListeners = new ArrayList<BindingListenerMetadata>();
    }

    public ServiceReferenceComponentMetadataImpl(ServiceReferenceComponentMetadata source) {
        super(source);
        if (source.getBindingListeners() != null) {
            bindingListeners = new ArrayList<BindingListenerMetadata>();
            Iterator i = source.getBindingListeners().iterator();
            while (i.hasNext()) {
                bindingListeners.add(new BindingListenerMetadataImpl((BindingListenerMetadata)i.next()));
            }
        }
        if (source.getInterfaceNames() != null) {
            interfaceNames = new HashSet<String>(source.getInterfaceNames());
        }
        serviceAvailabilitySpecification = source.getServiceAvailabilitySpecification();
        filter = source.getFilter();
        componentName = source.getComponentName();
    }
    
    public int getServiceAvailabilitySpecification() {
        return serviceAvailabilitySpecification;
    }

    public void setServiceAvailabilitySpecification(int serviceAvailabilitySpecification) {
        this.serviceAvailabilitySpecification = serviceAvailabilitySpecification;
    }

    public Set<String> getInterfaceNames() {
        return interfaceNames;
    }

    public void setInterfaceNames(Set<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
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

    public Collection<BindingListenerMetadata> getBindingListeners() {
        return Collections.unmodifiableCollection(bindingListeners);
    }

    public void addBindingListener(BindingListenerMetadata bindingListenerMetadata) {
        bindingListeners.add(bindingListenerMetadata);
    }
}
