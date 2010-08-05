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
package org.apache.aries.transaction.parsing;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.transaction.Constants;
import org.apache.aries.transaction.TxComponentMetaDataHelper;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TxElementHandler implements NamespaceHandler {
    private static final Logger LOGGER =
        LoggerFactory.getLogger(TxElementHandler.class);

    private TxComponentMetaDataHelper metaDataHelper;
    private Interceptor interceptor = null;

    private Set<ComponentDefinitionRegistry> registered = new HashSet<ComponentDefinitionRegistry>();

    private void parseElement(Element elt, ComponentMetadata cm, ParserContext pc)
    {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("parser asked to parse .. " + elt);

        if ("transaction".equals(elt.getLocalName())) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("parser adding interceptor for " + elt);

            ComponentDefinitionRegistry cdr = pc.getComponentDefinitionRegistry();
            
            if (cm == null) {
                // if the enclosing component is null, then we assume this is the top element 
                
                
                String bean = elt.getAttribute(Constants.BEAN);
                registerComponentsWithInterceptor(cdr, bean);

                metaDataHelper.populateBundleWideTransactionData(pc.getComponentDefinitionRegistry(), 
                        elt.getAttribute(Constants.VALUE), elt.getAttribute(Constants.METHOD), bean);
            } else {
                cdr.registerInterceptorWithComponent(cm, interceptor);
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("parser setting comp trans data for " + elt.getAttribute(Constants.VALUE) + "  "
                            + elt.getAttribute(Constants.METHOD));
    
                metaDataHelper.setComponentTransactionData(cm, elt.getAttribute(Constants.VALUE), elt
                        .getAttribute(Constants.METHOD));
            }
        }
        
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("parser done with " + elt);
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata cm, ParserContext pc)
    {
        if (node instanceof Element) {
            Element elt = (Element) node;
            parseElement(elt, cm, pc);
        }
        return cm;
    }

    public Metadata parse(Element elt, ParserContext pc)
    {
        parseElement(elt, pc.getEnclosingComponent(), pc);
        return null;
    }

    public URL getSchemaLocation(String arg0)
    {
    	if (arg0.equals(Constants.TRANSACTION10URI)) {
    		return this.getClass().getResource(Constants.TX10_SCHEMA);
    	} else {
            return this.getClass().getResource(Constants.TX11_SCHEMA);
    	}
    }

    public final void setTxMetaDataHelper(TxComponentMetaDataHelper transactionEnhancer)
    {
        this.metaDataHelper = transactionEnhancer;
    }

    public final void setTransactionInterceptor(Interceptor itx)
    {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("parser having interceptor set " + itx);
        
        this.interceptor = itx;
    }

    public Set<Class> getManagedClasses()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    private boolean isRegistered(ComponentDefinitionRegistry cdr) {
        for (ComponentDefinitionRegistry compdr : registered) {
            if (compdr == cdr) {
                return true;
            }
        }
        
        return false;
    }
    
    private void registerComponentsWithInterceptor(ComponentDefinitionRegistry cdr, String bean) {
        // if it is already registered all components in the component definition registry, do nothing
        if (isRegistered(cdr)) {
            return;
        }
        
        Set<String> ids = cdr.getComponentDefinitionNames();
        
        if (bean == null || bean.isEmpty()) {
            // in this case, let's attempt to register all components
            // if the component has already been registered with this interceptor,
            // the registration will be ignored.
            for (String id : ids) {
                ComponentMetadata componentMetadata = cdr.getComponentDefinition(id);
                cdr.registerInterceptorWithComponent(componentMetadata, interceptor);
            }
            synchronized (registered) {
                registered.add(cdr);
            }
        } else {
            // register the beans specified
            Pattern p = Pattern.compile(bean);
            for (String id : ids) {
                Matcher m = p.matcher(id);
                if (m.matches()) {
                    ComponentMetadata componentMetadata = cdr.getComponentDefinition(id);
                    cdr.registerInterceptorWithComponent(componentMetadata, interceptor);
                }
            }
        }
    }
    
    // check if the beans pattern includes the particular component/bean id
    /*private boolean includes(String beans, String id) {
        return Pattern.matches(beans, id);
    }*/
}
