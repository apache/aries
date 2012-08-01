/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.services;

import java.security.AccessControlContext;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Processor;
import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.proxy.ProxyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.reflect.BeanMetadata;

/**
 * TODO: javadoc
 *
 * @version $Rev$, $Date$
 */
public interface ExtendedBlueprintContainer extends BlueprintContainer {

    BundleContext getBundleContext();

    Bundle getExtenderBundle();

    BlueprintListener getEventDispatcher();

    Converter getConverter();

    Class loadClass(String name) throws ClassNotFoundException;

    ComponentDefinitionRegistry getComponentDefinitionRegistry();

    <T extends Processor> List<T> getProcessors(Class<T> type);

    /**
     * To be removed as internal API
     */
    @Deprecated
    Repository getRepository();
    
    ServiceRegistration registerService(String[] classes, Object service, Dictionary properties);
    
    Object getService(ServiceReference reference);
    
    AccessControlContext getAccessControlContext();

    void reload();

    ProxyManager getProxyManager();
    

    /**
     * Inject (or reinject) an Object instance with the blueprint properties defined by a BeanMetadata
     * 
     * Throws IllegalArgumentException if the bean metadata does not exist in this blueprint container
     * Throws ComponentDefinitionException if the injection process fails - this may have rendered the supplied Object unusable by partially completing the injection process
     */
    void injectBeanInstance(BeanMetadata bmd, Object o) 
        throws IllegalArgumentException, ComponentDefinitionException;

    ExecutorService getExecutors();

}
