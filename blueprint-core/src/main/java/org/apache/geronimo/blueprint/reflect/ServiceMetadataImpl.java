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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.Target;

/**
 * Implementation of ServiceMetadata
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ServiceMetadataImpl extends ComponentMetadataImpl implements ServiceMetadata {

    private Target exportedComponent;
    private List<String> interfaceNames;
    private int autoExportMode;
    private List<MapEntry> serviceProperties;
    private int ranking;
    private Collection<RegistrationListener> registrationListeners;
    private List<String> explicitDependencies;

    public ServiceMetadataImpl() {
    }
    
    public ServiceMetadataImpl(ServiceMetadata source) {
        super(source);
        this.exportedComponent = MetadataUtil.cloneTarget(source.getServiceComponent());
        this.interfaceNames = new ArrayList<String>(source.getInterfaceNames());
        this.autoExportMode = source.getAutoExportMode();
        for (MapEntry serviceProperty : source.getServiceProperties()) {
            addServiceProperty(new MapEntryImpl(serviceProperty));
        }
        this.ranking = source.getRanking();
        for (RegistrationListener listener : source.getRegistrationListeners()) {
            addRegistrationListener(new RegistrationListenerImpl(listener));
        }
        this.explicitDependencies = new ArrayList<String>(source.getExplicitDependencies());
    }

    public Target getServiceComponent() {
        return exportedComponent;
    }

    public void setExportedComponent(Target exportedComponent) {
        this.exportedComponent = exportedComponent;
    }

    public List<String> getInterfaceNames() {
        if (this.interfaceNames == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(this.interfaceNames);
        }
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

    public int getAutoExportMode() {
        return this.autoExportMode;
    }

    public void setAutoExportMode(int autoExportMode) {
        this.autoExportMode = autoExportMode;
    }

    public List<MapEntry> getServiceProperties() {
        if (this.serviceProperties == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(this.serviceProperties);
        }
    }

    public void setServiceProperties(List<MapEntry> serviceProperties) {
        this.serviceProperties = serviceProperties;
    }

    public void addServiceProperty(MapEntry serviceProperty) {
        if (this.serviceProperties == null) {
            this.serviceProperties = new ArrayList<MapEntry>();
        }
        this.serviceProperties.add(serviceProperty);
    }
    
    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public Collection<RegistrationListener> getRegistrationListeners() {
        if (this.registrationListeners == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableCollection(this.registrationListeners);
        }
    }

    public void setRegistrationListeners(Collection<RegistrationListener> registrationListeners) {
        this.registrationListeners = registrationListeners;
    }

    public void addRegistrationListener(RegistrationListener registrationListenerMetadata) {
        if (this.registrationListeners == null) {
            this.registrationListeners = new ArrayList<RegistrationListener>();
        }
        this.registrationListeners.add(registrationListenerMetadata);
    }

    public List<String> getExplicitDependencies() {
        return this.explicitDependencies;
    }

    public void setExplicitDependencies(List<String> explicitDependencies) {
        this.explicitDependencies = explicitDependencies;
    }

    public void addExplicitDependency(String explicitDependency) {
        if (this.explicitDependencies == null) {
            this.explicitDependencies = new ArrayList<String>();
        }
        this.explicitDependencies.add(explicitDependency);
    }

}
