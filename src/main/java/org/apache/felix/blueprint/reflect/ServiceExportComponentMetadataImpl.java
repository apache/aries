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
package org.apache.felix.blueprint.reflect;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.osgi.service.blueprint.reflect.MapValue;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ServiceExportComponentMetadataImpl extends ComponentMetadataImpl implements ServiceExportComponentMetadata {

    private Value exportedComponent;
    private Set<String> interfaceNames;
    private int autoExportMode;
    private Map serviceProperties;    
    private MapValue servicePropertiesValue;
    private int ranking;
    private Collection<RegistrationListenerMetadata> registrationListeners;

    public ServiceExportComponentMetadataImpl() {        
    }
    
    public ServiceExportComponentMetadataImpl(ServiceExportComponentMetadata source) {
        super(source);
        if (source.getServiceProperties() != null) {
            serviceProperties = new HashMap(source.getServiceProperties());
        }
        exportedComponent = MetadataUtil.cloneValue(source.getExportedComponent());
        ranking = source.getRanking();
        if (source.getInterfaceNames() != null) {
            interfaceNames = new HashSet<String>(source.getInterfaceNames());
        }
        if (source.getRegistrationListeners() != null) {
            Iterator i = source.getRegistrationListeners().iterator();
            registrationListeners = new ArrayList<RegistrationListenerMetadata>();
            while (i.hasNext()) {
                registrationListeners.add(new RegistrationListenerMetadataImpl((RegistrationListenerMetadata)i.next()));
            }
        }
        autoExportMode = source.getAutoExportMode();
    }
    
    public Value getExportedComponent() {
        return exportedComponent;
    }

    public void setExportedComponent(Value exportedComponent) {
        this.exportedComponent = exportedComponent;
    }

    public Set<String> getInterfaceNames() {
        return Collections.unmodifiableSet(interfaceNames);
    }

    public void setInterfaceNames(Set<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
    }

    public int getAutoExportMode() {
        return autoExportMode;
    }

    public void setAutoExportMode(int autoExportMode) {
        this.autoExportMode = autoExportMode;
    }

    public Map getServiceProperties() {
        return serviceProperties;
    }

    public void setServiceProperties(Map serviceProperties) {
        this.serviceProperties = serviceProperties;
    }
    
    public MapValue getServicePropertiesValue() {
        return servicePropertiesValue;
    }
    
    public void setServicePropertiesValue(MapValue servicePropertiesValue) {
        this.servicePropertiesValue = servicePropertiesValue;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public Collection<RegistrationListenerMetadata> getRegistrationListeners() {
        return Collections.unmodifiableCollection(registrationListeners);
    }

    public void setRegistrationListeners(Collection<RegistrationListenerMetadata> registrationListeners) {
        this.registrationListeners = registrationListeners;
    }

    public void addRegistrationListener(RegistrationListenerMetadata registrationListenerMetadata) {
        if (this.registrationListeners == null) {
            this.registrationListeners = new ArrayList<RegistrationListenerMetadata>();
        }
        this.registrationListeners.add(registrationListenerMetadata);
    }
}
