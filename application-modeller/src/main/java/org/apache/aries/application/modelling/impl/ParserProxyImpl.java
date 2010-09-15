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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.ParsedServiceElements;
import org.apache.aries.application.modelling.ParserProxy;
import org.apache.aries.application.modelling.WrappedServiceMetadata;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ParserService;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserProxyImpl implements ParserProxy {
  private Logger _logger = LoggerFactory.getLogger(ParserProxyImpl.class);
  private ParserService _parserService;
  private BundleContext _bundleContext;
  private ModellingManager _modellingManager;
  
  public void setParserService (ParserService p) { 
    _parserService = p;
  }
  
  public void setBundleContext (BundleContext b) { 
    _bundleContext = b;
  }
  
  public void setModellingManager (ModellingManager m) { 
    _modellingManager = m;
  }
  
  @Override
  public List<? extends WrappedServiceMetadata> parse(List<URL> blueprintsToParse) throws Exception {
    _logger.debug(LOG_ENTRY, "parse", new Object[]{blueprintsToParse});
    ComponentDefinitionRegistry cdr = _parserService.parse (blueprintsToParse, _bundleContext.getBundle());
    List<? extends WrappedServiceMetadata> result = parseCDRForServices (cdr, true);
    _logger.debug(LOG_EXIT, "parse", new Object[]{result});
    return result;
  }
   
  @Override
  public List<? extends WrappedServiceMetadata> parse(URL blueprintToParse) throws Exception {
    _logger.debug(LOG_ENTRY, "parse", new Object[]{blueprintToParse});
    List<URL> list = new ArrayList<URL>();
    list.add(blueprintToParse);
   
    List<? extends WrappedServiceMetadata> result = parse (list);
    _logger.debug(LOG_EXIT, "parse", new Object[]{result});
    return result;
  }

  @Override
  public List<? extends WrappedServiceMetadata> parse(InputStream blueprintToParse) throws Exception {
    _logger.debug(LOG_ENTRY, "parse", new Object[]{blueprintToParse});
    ComponentDefinitionRegistry cdr = _parserService.parse (blueprintToParse, _bundleContext.getBundle());
    List<? extends WrappedServiceMetadata> result = parseCDRForServices (cdr, true);
    _logger.debug(LOG_EXIT, "parse", new Object[]{result});
    return result;
  }
 

  @Override
  public ParsedServiceElements parseAllServiceElements(InputStream blueprintToParse) throws Exception {
    _logger.debug(LOG_ENTRY, "parseAllServiceElements", new Object[]{blueprintToParse});
    ComponentDefinitionRegistry cdr = _parserService.parse (blueprintToParse, _bundleContext.getBundle());
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
    Set<String> names = cdr.getComponentDefinitionNames();
    for (String name: names) { 
      ComponentMetadata compMetadata = cdr.getComponentDefinition(name);
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
            List<Metadata> values = ((CollectionMetadata)value).getValues();
            String[] theseValues = new String[values.size()];
            for (int i=0; i < values.size(); i++) { 
              Metadata m = values.get(i); 
              theseValues[i] = ((ValueMetadata)m).getStringValue();
            }
            serviceProps.put(key, theseValues);
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

  /**
   * Extract References metadata from a ComponentDefinitionRegistry. 
   * @param cdr                       ComponentDefinitionRegistry
   * @return List<WrappedReferenceMetadata>
   * @throws InvalidAttributeException 
   */
  private List<ImportedService> parseCDRForReferences (ComponentDefinitionRegistry cdr) throws InvalidAttributeException { 
    _logger.debug(LOG_ENTRY, "parseCDRForReferences", new Object[]{cdr});
    List<ImportedService> result = new ArrayList<ImportedService>();
    Set<String> names = cdr.getComponentDefinitionNames();
    for (String name: names) { 
      ComponentMetadata compMetadata = cdr.getComponentDefinition(name);
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
        if (isNotBlacklisted (iface, blueprintFilter)) { 
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
  private boolean isNotBlacklisted (String iface, String blueprintFilter) { 
    _logger.debug(LOG_ENTRY, "isNotBlacklisted", new Object[]{iface, blueprintFilter});
    boolean blacklisted = false;
    if (iface != null) {
      // JPA - detect interface;
      blacklisted |= iface.equals("javax.persistence.EntityManagerFactory");
      blacklisted |= iface.equals("javax.persistence.EntityManager");
    
      // JTA - detect interface
      blacklisted |= iface.equals("javax.transaction.UserTransaction");
      blacklisted |= iface.equals("javax.transaction.TransactionSynchronizationRegistry");
    }
    _logger.debug(LOG_EXIT, "isNotBlacklisted", new Object[]{!blacklisted});
    return !blacklisted;
  }

 
}
