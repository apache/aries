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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMFServiceRefFactory {
    private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.blueprint.aries");
    
    /** The blueprint attribute value to make a bean eager */
    private static final String ACTIVATION_EAGER = "EAGER";
    
    @SuppressWarnings("unchecked")
    ComponentMetadata create(boolean isPersistenceUnit,
        ParserContext ctx, String unitName) {
      // Create a service reference for the EMF (it is an EMF for persistence
      // contexts and units)
      final MutableReferenceMetadata refMetadata = ctx.createMetadata(MutableReferenceMetadata.class);
      refMetadata.setActivation(ACTIVATION_EAGER.equalsIgnoreCase(ctx
              .getDefaultActivation()) ? ReferenceMetadata.ACTIVATION_EAGER
              : ReferenceMetadata.ACTIVATION_LAZY);
      refMetadata.setAvailability(ReferenceMetadata.AVAILABILITY_MANDATORY);
      refMetadata.setInterface(EntityManagerFactory.class.getName());
      String filter = createEMFServiceFilter(isPersistenceUnit, unitName);
      refMetadata.setFilter(filter);
      refMetadata.setTimeout(Integer.parseInt(ctx.getDefaultTimeout()));
      refMetadata.setDependsOn((List<String>) Collections.EMPTY_LIST);
      refMetadata.setId(ctx.generateId());

      // Finally, if this is a persistence context we need to create the
      // entity manager as the Target
      ComponentMetadata target = isPersistenceUnit ? refMetadata
              : createInjectionBeanMetedata(ctx, refMetadata);
      return target;
    }

    private String createEMFServiceFilter(boolean isPersistenceUnit, String unitName) {
        // Pick the right EMF by looking for the presence, or absence, of the
        // PROXY_FACTORY service property
        StringBuilder filter = new StringBuilder("(&");
        // Persistence units do not have the property, persistence contexts do
        if (isPersistenceUnit)
            filter.append("(!(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
        else
            filter.append("(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=true)");

        // Add the empty name filter if necessary
        if (!"".equals(unitName))
            filter.append("(" + PersistenceUnitConstants.OSGI_UNIT_NAME + "=" + unitName + ")");
        else
            filter.append("(" + PersistenceUnitConstants.EMPTY_PERSISTENCE_UNIT_NAME + "=true)");

        filter.append(")");
        return filter.toString();
    }
    
    /**
     * This method turns a persistence context factory into an
     * {@link EntityManager} using blueprint factories
     * 
     * @param ctx
     *            the {@link ParserContext}
     * @param factory
     *            the reference bean for the persistence context factory
     * @return
     */
    private ComponentMetadata createInjectionBeanMetedata(ParserContext ctx,
        MutableReferenceMetadata factory) {

        if (_logger.isDebugEnabled())
            _logger.debug("Creating a managed persistence context definition for injection");

        //We want the EntityManager objects created from this factory to be damped too
        factory.setProxyChildBeanClasses(Arrays.asList(new Class<?>[]{EntityManager.class}));
        
        // Register the factory bean, and then create an entitymanager from it
        ctx.getComponentDefinitionRegistry().registerComponentDefinition(factory);

        MutableBeanMetadata meta = ctx.createMetadata(MutableBeanMetadata.class);
        MutableRefMetadata ref = ctx.createMetadata(MutableRefMetadata.class);
        ref.setComponentId(factory.getId());
        meta.setFactoryComponent(ref);
        meta.setActivation(factory.getActivation());
        meta.setFactoryMethod("createEntityManager");
        meta.setScope(BeanMetadata.SCOPE_PROTOTYPE);
        meta.setDestroyMethod("internalClose");

        return meta;
    }
}
