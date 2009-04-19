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
package org.apache.felix.blueprint.context;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;

import org.apache.felix.blueprint.BlueprintConstants;
import org.apache.felix.blueprint.HeaderParser;
import org.apache.felix.blueprint.HeaderParser.PathElement;
import org.apache.felix.blueprint.ModuleContextEventSender;
import org.apache.felix.blueprint.NamespaceHandlerRegistry;
import org.apache.felix.blueprint.convert.ConversionServiceImpl;
import org.apache.felix.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.xbean.recipe.ObjectGraph;
import org.apache.xbean.recipe.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.context.NoSuchComponentException;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.convert.Converter;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.Value;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ModuleContextImpl implements ModuleContext {

    private final BundleContext bundleContext;
    private final ModuleContextEventSender sender;
    private final NamespaceHandlerRegistry handlers;
    private final List<URL> urls;
    private final ComponentDefinitionRegistryImpl componentDefinitionRegistry;
    private final ConversionServiceImpl conversionService;
    private Map<String, Object> instances;
    private ServiceRegistration registration;

    public ModuleContextImpl(BundleContext bundleContext, ModuleContextEventSender sender, NamespaceHandlerRegistry handlers, List<URL> urls) {
        this.bundleContext = bundleContext;
        this.sender = sender;
        this.handlers = handlers;
        this.urls = urls;
        this.conversionService = new ConversionServiceImpl();
        this.componentDefinitionRegistry = new ComponentDefinitionRegistryImpl();
    }

    private void checkDirectives() {
        Bundle bundle = bundleContext.getBundle();
        Dictionary headers = bundle.getHeaders();
        String symbolicName = (String)headers.get(Constants.BUNDLE_SYMBOLICNAME);
        List<PathElement> paths = HeaderParser.parseHeader(symbolicName);
        String timeout = paths.get(0).getDirective(BlueprintConstants.TIMEOUT_DIRECTIVE);
        String waitForDependencies = paths.get(0).getDirective(BlueprintConstants.WAIT_FOR_DEPENDENCIES_DIRECTIVE);

        // TODO: hook this up
        
        if (timeout != null) {
            System.out.println("Timeout: " + timeout);
        }
        if (waitForDependencies != null) {
            System.out.println("Wait-for-dependencies: " + waitForDependencies);
        }
    }
    
    public void create() {
        checkDirectives();
        sender.sendCreating(this);
        try {
            Parser parser = new Parser(handlers, componentDefinitionRegistry, urls);
            parser.parse();
            Instanciator i = new Instanciator(this);
            Repository repository = i.createRepository(componentDefinitionRegistry);
            ObjectGraph graph = new ObjectGraph(repository);

            registerTypeConverters(graph);

            // TODO: handle scopes and such
            instances = graph.createAll(new ArrayList<String>(componentDefinitionRegistry.getComponentDefinitionNames()));
            System.out.println(instances);

            registerAllServices();
            
            // Register the ModuleContext in the OSGi registry
            Properties props = new Properties();
            props.put("osgi.blueprint.context.symbolicname", bundleContext.getBundle().getSymbolicName());
            props.put("osgi.blueprint.context.version", bundleContext.getBundle().getHeaders().get(Constants.BUNDLE_VERSION));
            registration = bundleContext.registerService(ModuleContext.class.getName(), this, props);

            sender.sendCreated(this);
        } catch (WaitForDependencyException e) {
            sender.sendWaiting(this, e.getServiceObjectClass(), e.getServiceFilter());
            // TODO: wait for dependency
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: pass the exception to the event
            sender.sendFailure(this, e);
        }
    }

    private void registerTypeConverters(ObjectGraph graph) {
        List<String> typeConvertersNames = componentDefinitionRegistry.getTypeConverterNames();
        Map<String, Object> typeConverters = graph.createAll(typeConvertersNames);
        System.out.println(typeConverters);
        for (String name : typeConvertersNames) {
            Object typeConverterInstance = typeConverters.get(name);
            if (typeConverterInstance instanceof Converter) {
                Converter converter = (Converter) typeConverterInstance;
                conversionService.registerConverter(converter);
            } else {
                // TODO: throw exception or log
            }
        }
    }
    
    private void registerAllServices() {
        for (ServiceExportComponentMetadata service : getExportedServicesMetadata()) {
            ServiceRegistrationProxy proxy = (ServiceRegistrationProxy) getComponent(service.getName());
            proxy.register();
        }
    }
    
    private void unregisterAllServices() {
        for (ServiceExportComponentMetadata service : getExportedServicesMetadata()) {
            ServiceRegistrationProxy proxy = (ServiceRegistrationProxy) getComponent(service.getName());
            proxy.unregister();
        }
    }
    
    public Set<String> getComponentNames() {
        return componentDefinitionRegistry.getComponentDefinitionNames();
    }
    
    public Object getComponent(String name) throws NoSuchComponentException {
        ComponentMetadata metadata = getComponentMetadata(name);
        if (metadata == null) {
            throw new NoSuchComponentException(name);
        }
        // TODO: handle scopes and such
        return instances.get(name);
    }

    public ComponentMetadata getComponentMetadata(String name) {
        ComponentMetadata metadata = componentDefinitionRegistry.getComponentDefinition(name);
        if (metadata == null) {
            throw new NoSuchComponentException(name);
        }
        return metadata;
    }

    public Collection<ServiceReferenceComponentMetadata> getReferencedServicesMetadata() {
        return getMetadata(ServiceReferenceComponentMetadata.class);
    }

    public Collection<ServiceExportComponentMetadata> getExportedServicesMetadata() {
        return getMetadata(ServiceExportComponentMetadata.class);
    }

    public Collection<LocalComponentMetadata> getLocalComponentsMetadata() {
        return getMetadata(LocalComponentMetadata.class);
    }

    private <T> Collection<T> getMetadata(Class<T> clazz) {
        Collection<T> metadatas = new ArrayList<T>();
        for (String name : componentDefinitionRegistry.getComponentDefinitionNames()) {
            ComponentMetadata component = componentDefinitionRegistry.getComponentDefinition(name);
            if (clazz.isInstance(component)) {
                metadatas.add(clazz.cast(component));
            }
        }
        metadatas = Collections.unmodifiableCollection(metadatas);
        return metadatas;

    }

    public ConversionService getConversionService() {
        return conversionService;
    }
    
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void destroy() {
        if (registration != null) {
            registration.unregister();
        }
        sender.sendDestroying(this);
        unregisterAllServices();
        System.out.println("Module context destroyed: " + this.bundleContext);
        // TODO: destroy all instances
        sender.sendDestroyed(this);
    }

}
