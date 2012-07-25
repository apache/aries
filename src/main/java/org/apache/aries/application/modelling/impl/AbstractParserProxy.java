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
package org.apache.aries.application.modelling.impl;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.ParsedServiceElements;
import org.apache.aries.application.modelling.ParserProxy;
import org.apache.aries.application.modelling.WrappedServiceMetadata;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.osgi.service.jndi.JNDIConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractParserProxy implements ParserProxy {
	  private final Logger _logger = LoggerFactory.getLogger(AbstractParserProxy.class);
	  private ModellingManager _modellingManager;


      protected abstract ComponentDefinitionRegistry parseCDR(List<URL> blueprintsToParse) throws Exception;
      protected abstract ComponentDefinitionRegistry parseCDR(InputStream blueprintToParse) throws Exception;
	  
	  public void setModellingManager (ModellingManager m) { 
	    _modellingManager = m;
	  }

	  public List<? extends WrappedServiceMetadata> parse(List<URL> blueprintsToParse) throws Exception {
	    _logger.debug(LOG_ENTRY, "parse", new Object[]{blueprintsToParse});
	    ComponentDefinitionRegistry cdr = parseCDR (blueprintsToParse);
	    List<? extends WrappedServiceMetadata> result = parseCDRForServices (cdr, true);
	    _logger.debug(LOG_EXIT, "parse", new Object[]{result});
	    return result;
	  }
	   
	  public List<? extends WrappedServiceMetadata> parse(URL blueprintToParse) throws Exception {
	    _logger.debug(LOG_ENTRY, "parse", new Object[]{blueprintToParse});
	    List<URL> list = new ArrayList<URL>();
	    list.add(blueprintToParse);
	   
	    List<? extends WrappedServiceMetadata> result = parse (list);
	    _logger.debug(LOG_EXIT, "parse", new Object[]{result});
	    return result;
	  }

	  public List<? extends WrappedServiceMetadata> parse(InputStream blueprintToParse) throws Exception {
	    _logger.debug(LOG_ENTRY, "parse", new Object[]{blueprintToParse});
	    ComponentDefinitionRegistry cdr = parseCDR (blueprintToParse);
	    List<? extends WrappedServiceMetadata> result = parseCDRForServices (cdr, true);
	    _logger.debug(LOG_EXIT, "parse", new Object[]{result});
	    return result;
	  }
	 

	  public ParsedServiceElements parseAllServiceElements(InputStream blueprintToParse) throws Exception {
	    _logger.debug(LOG_ENTRY, "parseAllServiceElements", new Object[]{blueprintToParse});
	    ComponentDefinitionRegistry cdr = parseCDR (blueprintToParse);
	    Collection<ExportedService> services = parseCDRForServices(cdr, false);
	    Collection<ImportedService> references = parseCDRForReferences (cdr);
	    ParsedServiceElements result = _modellingManager.getParsedServiceElements(services, references);
	    _logger.debug(LOG_EXIT, "parseAllServiceElements", new Object[]{result});
	    return result;
	  }
	  
	  /**
	   * Extract Service metadata from a ComponentDefinitionRegistry. When doing SCA modelling, we
	   * need to suppress anonymous services. We don't want to do that when we're modelling for 
	   * provisioning dependencies. 
	   * @param cdr                       ComponentDefinitionRegistry
	   * @param suppressAnonymousServices Unnamed services will not be returned if this is true
	   * @return List<WrappedServiceMetadata>
	   */
	  private List<ExportedService> parseCDRForServices (ComponentDefinitionRegistry cdr, 
	      boolean suppressAnonymousServices) { 
	    _logger.debug(LOG_ENTRY, "parseCDRForServices", new Object[]{cdr, suppressAnonymousServices});
	    List<ExportedService> result = new ArrayList<ExportedService>();
	    for (ComponentMetadata compMetadata : findAllComponents(cdr)) { 
	      if (compMetadata instanceof ServiceMetadata) { 
	        ServiceMetadata serviceMetadata = (ServiceMetadata)compMetadata;
	        String serviceName;
	        int ranking;
	        Collection<String> interfaces = new ArrayList<String>(); 
	        Map<String, Object> serviceProps = new HashMap<String, Object>();

	        ranking = serviceMetadata.getRanking();
	        for (Object i : serviceMetadata.getInterfaces()) {
	          interfaces.add((String)i);
	        }
	        
	        // get the service properties
	        List<MapEntry> props = serviceMetadata.getServiceProperties();
	        for (MapEntry entry : props) { 
	          String key = ((ValueMetadata)entry.getKey()).getStringValue();
	          
	          Metadata value = entry.getValue();
	          if (value instanceof CollectionMetadata) { 
	            processMultiValueProperty(serviceProps, key, value);
	          } else { 
	            serviceProps.put(key, ((ValueMetadata)entry.getValue()).getStringValue());
	          }
	        }

	        // serviceName: use the service id unless that's not set, 
	        // in which case we use the bean id. 
	        serviceName = serviceMetadata.getId();
	        
	        // If the Service references a Bean, export the bean id as a service property
	        // as per 121.6.5 p669 of the blueprint 1.0 specification
	        Target t = serviceMetadata.getServiceComponent();
	        String targetId = null;
	        if (t instanceof RefMetadata) { 
	          targetId = ((RefMetadata)t).getComponentId();
	        } else if (t instanceof BeanMetadata) { 
	          targetId = ((BeanMetadata)t).getId();
	        }
	        
	        // Our OBR code MUST have access to targetId if it's available (i.e. not null 
	        // or auto-generated for an anonymous service. This must ALWAYS be set. 
	        if (targetId != null && !targetId.startsWith(".")) { // Don't set this for anonymous inner components
	            serviceProps.put("osgi.service.blueprint.compname", targetId);
	          if (serviceName == null || serviceName.equals("") || serviceName.startsWith(".")) { 
	            serviceName = targetId;
	          }
	        }
	        
	        if(serviceName != null && serviceName.startsWith("."))
	          serviceName = null;
	        
	        // If suppressAnonymous services, do not expose services that have no name
	        if (!suppressAnonymousServices || (serviceName != null)) { 
	          ExportedService wsm = _modellingManager.getExportedService(serviceName, ranking, interfaces, serviceProps);
	          result.add(wsm);
	        }
	      }
	    }
	    _logger.debug(LOG_EXIT, "parseAllServiceElements", new Object[]{result});
	    return result; 
	  }
    private void processMultiValueProperty(Map<String, Object> serviceProps,
        String key, Metadata value) {
      List<Metadata> values = ((CollectionMetadata)value).getValues();
      Class<?> collectionClass = ((CollectionMetadata)value).getCollectionClass();
      Object collectionValue;
      
      if(Collection.class.isAssignableFrom(collectionClass)) {
        Collection<String> theseValues = getCollectionFromClass(collectionClass);
        for(Metadata m : values) {
          theseValues.add(((ValueMetadata)m).getStringValue());
        }
        collectionValue = theseValues;
      } else {
        String[] theseValues = new String[values.size()];
        for (int i=0; i < values.size(); i++) { 
          Metadata m = values.get(i); 
          theseValues[i] = ((ValueMetadata)m).getStringValue();
        }
        collectionValue = theseValues;
      }
      serviceProps.put(key, collectionValue);
    }	  
	  
	  private Collection<String> getCollectionFromClass(Class<?> collectionClass) {
	    
	    if(List.class.isAssignableFrom(collectionClass)) {
	      return new ArrayList<String>();
	    } else if (Set.class.isAssignableFrom(collectionClass)) {
	      return new LinkedHashSet<String>();
	    } else if (Queue.class.isAssignableFrom(collectionClass)) {
	      //This covers Queue and Deque, which is caught by the isAssignableFrom check
	      //as a sub-interface of Queue
	      return new LinkedList<String>();
	    } else {
	      throw new IllegalArgumentException(collectionClass.getName());
	    }
	  }
	  
    /**
	   * Extract References metadata from a ComponentDefinitionRegistry. 
	   * @param cdr                       ComponentDefinitionRegistry
	   * @return List<WrappedReferenceMetadata>
	   * @throws InvalidAttributeException 
	   */
	  private List<ImportedService> parseCDRForReferences (ComponentDefinitionRegistry cdr) throws InvalidAttributeException { 
	    _logger.debug(LOG_ENTRY, "parseCDRForReferences", new Object[]{cdr});
	    List<ImportedService> result = new ArrayList<ImportedService>();
	    for (ComponentMetadata compMetadata : findAllComponents(cdr)) { 
	      if (compMetadata instanceof ServiceReferenceMetadata) { 
	        ServiceReferenceMetadata referenceMetadata = (ServiceReferenceMetadata)compMetadata;

	        boolean optional = referenceMetadata.getAvailability() == ServiceReferenceMetadata.AVAILABILITY_OPTIONAL;
	        String iface = referenceMetadata.getInterface();
	        String compName = referenceMetadata.getComponentName();
	        String blueprintFilter = referenceMetadata.getFilter();
	        String id = referenceMetadata.getId();
	        boolean isMultiple = (referenceMetadata instanceof ReferenceListMetadata);
	        
	        //The blueprint parser teams up with JPA and blueprint resource ref
	        // namespace handlers to give us service imports of the form, 
	        // objectClass=javax.persistence.EntityManagerFactory, org.apache.aries.jpa.proxy.factory=*, osgi.unit.name=blabber
	        //
	        // There will be no matching service for this reference. 
	        // For now we blacklist certain objectClasses and filters - this is a pretty dreadful thing to do. 
	        if (!isBlacklisted (iface, blueprintFilter)) { 
	          ImportedService ref = _modellingManager.getImportedService (optional, iface, compName, blueprintFilter, 
	              id, isMultiple);
	          result.add (ref);  
	        }
	      }
	    }
	    _logger.debug(LOG_EXIT, "parseCDRForReferences", new Object[]{result});
	    return result; 
	  }
	  
	  /**
	   * Find all the components in a given {@link ComponentDefinitionRegistry} this finds top-level
	   * components as well as their nested counter-parts. It may however not find components in custom namespacehandler 
	   * {@link ComponentMetadata} instances.
	   * 
	   * @param cdr The {@link ComponentDefinitionRegistry} to scan
	   * @return a {@link Set} of {@link ComponentMetadata}
	   */
	  private Set<ComponentMetadata> findAllComponents(ComponentDefinitionRegistry cdr) {
	      Set<ComponentMetadata> components = new HashSet<ComponentMetadata>();
	      
	      for (String name : cdr.getComponentDefinitionNames()) {
	          ComponentMetadata component = cdr.getComponentDefinition(name);
	          traverseComponent(component, components);
	      }
	      
	      return components;
	  }
	  
	  /**
	   * Traverse to find all nested {@link ComponentMetadata} instances
	   * @param metadata
	   * @param output
	   */
	  private void traverse(Metadata metadata, Set<ComponentMetadata> output) {
	      if (metadata instanceof ComponentMetadata) {
	          traverseComponent((ComponentMetadata) metadata, output);	          
	      } else if (metadata instanceof CollectionMetadata) {
	          CollectionMetadata collection = (CollectionMetadata) metadata;
	          
	          for (Metadata v : collection.getValues()) traverse(v, output);
	      } else if (metadata instanceof MapMetadata) {
	          MapMetadata map = (MapMetadata) metadata;
	          
	          for (MapEntry e : map.getEntries()) {
	              traverse(e.getKey(), output);
	              traverse(e.getValue(), output);
	          }
	      }
	  }
	  
	  /**
	   * Traverse {@link ComponentMetadata} instances to find all nested {@link ComponentMetadata} instances
	   * @param component
	   * @param output
	   */
	  private void traverseComponent(ComponentMetadata component, Set<ComponentMetadata> output) {
	      if (!!!output.add(component)) return;
	      
	      if (component instanceof BeanMetadata) {
	          BeanMetadata bean = (BeanMetadata) component;
	          
	          traverse(bean.getFactoryComponent(), output);

	          for (BeanArgument argument : bean.getArguments()) {
	              traverse(argument.getValue(), output);
	          }
	          
	          for (BeanProperty property : bean.getProperties()) {
	              traverse(property.getValue(), output);
	          }
	          
	      } else if (component instanceof ServiceMetadata) {
	          ServiceMetadata service = (ServiceMetadata) component;
	          	          
	          traverse(service.getServiceComponent(), output);
	          
	          for (RegistrationListener listener : service.getRegistrationListeners()) {
	               traverse(listener.getListenerComponent(), output);
	          }
	          
	          for (MapEntry e : service.getServiceProperties()) {
	              traverse(e.getKey(), output);
	              traverse(e.getValue(), output);
	          }
	          
	      } else if (component instanceof ServiceReferenceMetadata) {
	          ServiceReferenceMetadata reference = (ServiceReferenceMetadata) component;
	          
	          for (ReferenceListener listener : reference.getReferenceListeners()) {
	              traverse(listener.getListenerComponent(), output);
	          }
	      }
	  }
	
	  /**
	   * Some services are injected directly into isolated frameworks by default. We do 
	   * not need to model these services. They are not represented as ExportedServices 
	   * (Capabilities) in the various OBR registries, and so cannot be resolved against. 
	   * Since they are injected directly into each isolated framework, we do not need
	   * an entry in DEPLOYMENT.MF's Deployed-ImportService header for any of these 
	   * services. 
	   * 
	   * @param iface           The interface declared on a blueprint reference
	   * @param blueprintFilter The filter on the blueprint reference
	   * @return                True if the service is not 'blacklisted' and so may be exposed
	   *                        in the model being generated. 
	   */
	  protected boolean isBlacklisted (String iface, String blueprintFilter) { 
	    _logger.debug(LOG_ENTRY, "isBlacklisted", new Object[]{iface, blueprintFilter});
	    boolean blacklisted = false;
	    if (iface != null) {
	      // JPA - detect interface;
	      blacklisted |= iface.equals("javax.persistence.EntityManagerFactory");
	      blacklisted |= iface.equals("javax.persistence.EntityManager");
	    
	      // JTA - detect interface
	      blacklisted |= iface.equals("javax.transaction.UserTransaction");
	      blacklisted |= iface.equals("javax.transaction.TransactionSynchronizationRegistry");
	      
	      // ConfigurationAdmin - detect interface
	      blacklisted |= iface.equals("org.osgi.service.cm.ConfigurationAdmin");
	               
	      // Don't provision against JNDI references
	      if (blueprintFilter != null && blueprintFilter.trim().length() != 0) { 
	        Map<String, String> filter = ManifestHeaderProcessor.parseFilter(blueprintFilter);
	        blacklisted |= filter.containsKey(JNDIConstants.JNDI_SERVICENAME);
	      }
	    }
	    _logger.debug(LOG_EXIT, "isBlacklisted", new Object[]{!blacklisted});
	    return blacklisted;
	  }
}
