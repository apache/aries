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
package org.apache.aries.jpa.blueprint.aries.impl;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;

import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.jpa.container.sync.Synchronization;
import org.apache.aries.util.nls.MessageUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class handles the JPA namespace in blueprint xml files, it configures
 * injection for managed persistence units and managed persistence contexts. The
 * namespace handler also registers clients of managed persistence contexts with
 * the {@link GlobalPersistenceManager}.
 */
@SuppressWarnings("rawtypes")
public class NSHandler implements NamespaceHandler {
    private static final String ATTR_INDEX = "index";

    /** Logger */
    private static final Logger _logger = LoggerFactory
            .getLogger("org.apache.aries.jpa.blueprint.aries");
    /** MessageUtil */
    private static final MessageUtil MESSAGES = MessageUtil.createMessageUtil(NSHandler.class, "org.apache.aries.jpa.blueprint.aries.nls.ariesBlueprintJpaMessages");

    /** The JPA 1.0.0 namespace */
    public static final String NS_URI_100 = "http://aries.apache.org/xmlns/jpa/v1.0.0";
    /** The JPA 1.0.0 namespace */
    public static final String NS_URI_110 = "http://aries.apache.org/xmlns/jpa/v1.1.0";
    /** The standard blueprint namespace */
    private static final String BLUEPRINT_NS = "http://www.osgi.org/xmlns/blueprint/v1.0.0";

    /**
     * The element name for an injected persistence unit (see
     * {@link PersistenceUnit})
     */
    private static final String TAG_UNIT = "unit";
    /**
     * The element name for an injected persistence context (see
     * {@link PersistenceContext})
     */
    private static final String TAG_CONTEXT = "context";
    /** The element name for a blueprint map */
    private static final String TAG_MAP = "map";

    /** The jpa attribute for property injection, provides the injection site */
    private static final String ATTR_PROPERTY = "property";
    /** The {@link PersistenceContextType} of a persistence context */
    private static final String ATTR_TYPE = "type";
    /** The name of the persistence unit */
    private static final String ATTR_UNIT_NAME = "unitname";
    /** The default name to use if no unit name is specified */
    private static final String DEFAULT_UNIT_NAME = "";
    
    /** The {@link PersistenceManager} to register contexts with */
    private PersistenceContextProvider manager;
    /** Used to indicate whether the PersistenceContextProvider is available */
    private final AtomicBoolean contextsAvailable = new AtomicBoolean();
    
    public void setManager(PersistenceContextProvider manager) {
        this.manager = manager;
    }

    /**
     * Called by blueprint when we meet a JPA namespace element
     */
    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata component,
            ParserContext context) {
        Element element = getValidNode(node, component);
        MutableBeanMetadata bean = getValidBean(component, element);

        String property = element.getAttribute(ATTR_PROPERTY);
        property = property.isEmpty() ? null : property;
        int index = parseIndex(element.getAttribute(ATTR_INDEX));
        String unitName = parseUnitName(element);
        boolean isPersistenceUnit = TAG_UNIT.equals(element.getLocalName());

        ComponentMetadata emfRef = new EMFServiceRefFactory().create(isPersistenceUnit, context, unitName);
        if(property != null && index != -1) {
            _logger.error(MESSAGES.getMessage("invalid.property.and.index"));
        } else if (property != null) {
            logPropertyInjection(property, unitName, isPersistenceUnit);
            bean.addProperty(property, emfRef);
        } else {
            logIndexInjection(index, unitName, isPersistenceUnit);
            String className = isPersistenceUnit ? EntityManagerFactory.class.getName() : EntityManager.class.getName();
            bean.addArgument(emfRef, className, index);
        }
        
        // If this is a persistence context then register it with the manager
        if (TAG_CONTEXT.equals(element.getLocalName())) {
            Bundle client = getBlueprintBundle(context);

            if (client != null) {
                HashMap<String, Object> properties = new HashMap<String, Object>();
                // Remember to add the PersistenceContextType so that we can create
                // the correct type of
                // EntityManager    
                properties.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, parseType(element));
                properties.putAll(parseJPAProperties(element, context));
                String clientInfo = client.getSymbolicName() + '/' + client.getVersion();
                if (contextsAvailable.get()) {
                    manager.registerContext(unitName, client, properties);
                } else {
                    _logger.warn(MESSAGES.getMessage("no.persistence.context.provider", clientInfo, unitName, properties));
                }
                Synchronization sync = getSyncService(unitName, client);
                if (sync != null) {
                    context.getComponentDefinitionRegistry().registerInterceptorWithComponent(component, createSyncInterceptor(sync));
                } else {
                    _logger.error(MESSAGES.getMessage("no.synchronization.registered", clientInfo, unitName, properties));
                }
            } else {
                _logger.debug("No bundle: this must be a dry, parse only run.");
            }
        }

        return bean;
    }

    private int parseIndex(String indexSt) {
        if (indexSt == null || indexSt.isEmpty()) {
            return -1;
        } else {
            try {
                return Integer.parseInt(indexSt);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(MESSAGES.getMessage("index.not.a.number", indexSt), nfe);
            }
        }
    }

    private void logPropertyInjection(String property, String unitName, boolean isPersistenceUnit) {
        if (_logger.isDebugEnabled()) {
              if (isPersistenceUnit)
                  _logger.debug("Creating blueprint injection metadata to inject the unit {} into bean property {}",
                                  new Object[] { unitName, property });
              else
                  _logger.debug("Creating blueprint injection metadata to inject the context {} into bean property {}",
                                  new Object[] { unitName, property });
          }
    }

    private void logIndexInjection(Integer index, String unitName, boolean isPersistenceUnit) {
        if (_logger.isDebugEnabled()) {
            if (isPersistenceUnit) {
                if (index == null)
                    _logger.debug("Creating blueprint injection metadata to inject the unit {} as a constructor argument",
                                new Object[] { unitName });
                else
                    _logger.debug("Creating blueprint injection metadata to inject the unit {} as a constructor argument" +
                        " with index {}", new Object[] { unitName, index });
            } else {
                if (index == null)
                    _logger.debug("Creating blueprint injection metadata to inject the context {} as a constructor argument",
                                new Object[] { unitName });
                else
                    _logger.debug("Creating blueprint injection metadata to inject the context {} as a constructor argument" +
                        " with index {}", new Object[] { unitName, index });
            }
        }
    }

    private MutableBeanMetadata getValidBean(ComponentMetadata component, Element element) {
        MutableBeanMetadata bean = (MutableBeanMetadata) component;

        if (!NS_URI_100.equals(element.getNamespaceURI())
            && !NS_URI_110.equals(element.getNamespaceURI())) {
            String message = MESSAGES.getMessage("unexpected.namespace", element.getNamespaceURI());
            _logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (!TAG_UNIT.equals(element.getLocalName())
                && !TAG_CONTEXT.equals(element.getLocalName())) {
            String message = MESSAGES.getMessage("unexpected.element", element.getLocalName());
            _logger.error(message);
            throw new IllegalArgumentException(message);
        }
        return bean;
    }

    private Element getValidNode(Node node, ComponentMetadata component) {
        // The node should always be an element
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            _logger.error(MESSAGES.getMessage("unexpected.node", node));
            throw new IllegalArgumentException(node.toString());
        }

        Element element = (Element) node;
        // The surrounding component should always be a bean
        if (!(component instanceof BeanMetadata)) {
            _logger.error(MESSAGES.getMessage("incorrect.component.type", component));
            throw new IllegalArgumentException(component.toString());
        }
        
        if (!(component instanceof MutableBeanMetadata)) {
            _logger.error(MESSAGES.getMessage("non.mutable.bean", component));
            throw new IllegalArgumentException(component.toString());
        }
        return element;
    }

    private Synchronization getSyncService(String unitName, Bundle client) {
        try {
            String filter = "(" + PersistenceUnitConstants.OSGI_UNIT_NAME + "=" + unitName + ")";
            Collection<ServiceReference<Synchronization>> refs = client.getBundleContext().getServiceReferences(Synchronization.class, filter);
            if (refs.size() > 0) {
                return client.getBundleContext().getService(refs.iterator().next());
            }
        } catch (InvalidSyntaxException e) {
            // Ignore
        }
        return null;
    }

    private Interceptor createSyncInterceptor(final Synchronization sync) {
        return new Interceptor() {
            @Override
            public Object preCall(ComponentMetadata componentMetadata, Method method, Object... objects) throws Throwable {
                sync.preCall();
                return null;
            }
            @Override
            public void postCallWithReturn(ComponentMetadata componentMetadata, Method method, Object o, Object o2) throws Throwable {
                sync.postCall();
            }
            @Override
            public void postCallWithException(ComponentMetadata componentMetadata, Method method, Throwable throwable, Object o) throws Throwable {
                sync.postCall();
            }
            @Override
            public int getRank() {
                return 0;
            }
        };
    }

    @Override
    public Set<Class> getManagedClasses() {
        // This is a no-op
        return null;
    }

    @Override
    public URL getSchemaLocation(String namespace) {
        if(NS_URI_100.equals(namespace))
            return getClass().getResource("/org/apache/aries/jpa/blueprint/namespace/jpa.xsd");
        else if (NS_URI_110.equals(namespace))
            return getClass().getResource("/org/apache/aries/jpa/blueprint/namespace/jpa_110.xsd");
        else
            return null;
    }

    @Override
    public Metadata parse(Element element, ParserContext context) {
        /*
         * The namespace does not define any top-level elements, so we should
         * never get here. In case we do -> explode.
         */
        _logger.error(MESSAGES.getMessage("unexpected.top.level.element"));
        throw new UnsupportedOperationException();
    }
    
    /**
     * Called when a {@link PersistenceContextProvider} is available
     * @param ref
     */
    public void contextAvailable(ServiceReference ref) {
        boolean log = contextsAvailable.compareAndSet(false, true);
      
        if(log && _logger.isDebugEnabled())
            _logger.debug("Managed persistence context support is now available for use with the Aries Blueprint container");
    }

    /**
     * Called when a {@link PersistenceContextProvider} is no longer available
     * @param ref
     */
    public void contextUnavailable(ServiceReference ref) {
        contextsAvailable.set(false);
        _logger.warn(MESSAGES.getMessage("jpa.support.gone"));
    }
    
    

    /**
     * Get hold of the blueprint bundle using the built in components
     * 
     * @param context
     * @return
     */
    private Bundle getBlueprintBundle(ParserContext context) {
        PassThroughMetadata metadata = (PassThroughMetadata) context
                .getComponentDefinitionRegistry().getComponentDefinition(
                        "blueprintBundle");

        Bundle result = null;
        if (metadata != null) {
            result = (Bundle) metadata.getObject();
        }

        return result;
    }

    private PersistenceContextType parseType(Element element) {
        if (element.hasAttribute(ATTR_TYPE))
            return PersistenceContextType.valueOf(element
                    .getAttribute(ATTR_TYPE));
        else
            return PersistenceContextType.TRANSACTION;
    }

    private String parseUnitName(Element element) {
        return element.hasAttribute(ATTR_UNIT_NAME) ? element
                .getAttribute(ATTR_UNIT_NAME) : DEFAULT_UNIT_NAME;
    }

    /**
     * Parse any properties for creating the persistence context
     * 
     * @param element
     * @param context
     * @return
     */
    private Map<String, Object> parseJPAProperties(Element element,
            ParserContext context) {
        Map<String, Object> result = new HashMap<String, Object>();
        NodeList ns = null;
        if(NS_URI_100.equals(element.getNamespaceURI())) {
            ns = element.getElementsByTagNameNS(NS_URI_100, TAG_MAP);
        } else {
            ns = element.getElementsByTagNameNS(NS_URI_110, TAG_MAP);
        }
        // Use the parser context to parse the map for us
        for (int i = 0; i < ns.getLength(); i++) {
            MapMetadata metadata = context.parseElement(MapMetadata.class,
                    null, (Element) ns.item(i));
            for (MapEntry entry : (List<MapEntry>) metadata.getEntries()) {
                if (entry.getKey() instanceof ValueMetadata
                        && entry.getValue() instanceof ValueMetadata) {
                    ValueMetadata key = (ValueMetadata) entry.getKey();
                    ValueMetadata value = (ValueMetadata) entry.getValue();

                    result.put(key.getStringValue(), value.getStringValue());
                } else {
                    _logger.error(MESSAGES.getMessage("map.not.well.formed"));
                    throw new UnsupportedOperationException();
                }
            }
        }

        return result;
    }
}
