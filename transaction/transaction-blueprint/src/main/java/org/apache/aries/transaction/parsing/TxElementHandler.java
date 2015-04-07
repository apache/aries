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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.aries.transaction.BundleWideTxData;
import org.apache.aries.transaction.Constants;
import org.apache.aries.transaction.TxComponentMetaDataHelper;
import org.apache.aries.transaction.annotations.TransactionPropagationType;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TxElementHandler implements NamespaceHandler {
    public static final String DEFAULT_INTERCEPTOR_ID = "txinterceptor";
    public static final String INTERCEPTOR_BLUEPRINT_ID = "interceptor.blueprint.id";

    private static final Logger LOGGER = LoggerFactory.getLogger(TxElementHandler.class);

    private TxComponentMetaDataHelper metaDataHelper;
    private Interceptor interceptor = null;

    private final ConcurrentMap<ComponentDefinitionRegistry,Bundle> registered = new ConcurrentHashMap<ComponentDefinitionRegistry, Bundle>();

    private void parseElement(Element elt, ComponentMetadata cm, ParserContext pc)
    {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("parser asked to parse .. " + elt);

        ComponentDefinitionRegistry cdr = pc.getComponentDefinitionRegistry();
        if ("transaction".equals(elt.getLocalName())) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("parser adding interceptor for " + elt);

            ComponentMetadata meta = cdr.getComponentDefinition("blueprintBundle");
            Bundle blueprintBundle = null;
            if (meta instanceof PassThroughMetadata) {
                blueprintBundle = (Bundle) ((PassThroughMetadata) meta).getObject();
            }

            // don't register components if we have no bundle (= dry parse)
            if (blueprintBundle != null) {
              registered.put(cdr, blueprintBundle);
              TransactionPropagationType value = getType(elt.getAttribute(Constants.VALUE));
              String method = elt.getAttribute(Constants.METHOD);
              if (cm == null) {
                  // if the enclosing component is null, then we assume this is the top element                 
                  
                  String bean = elt.getAttribute(Constants.BEAN);
                  registerComponentsWithInterceptor(cdr, bean);
  
                  metaDataHelper.populateBundleWideTransactionData(pc.getComponentDefinitionRegistry(), 
                          value, method, bean);
              } else {
                  cdr.registerInterceptorWithComponent(cm, interceptor);
                  if (LOGGER.isDebugEnabled())
                      LOGGER.debug("parser setting comp trans data for " + value + "  " + method);
      
                  metaDataHelper.setComponentTransactionData(cdr, cm, value, method);
              }
            }
        } else if ("enable-annotations".equals(elt.getLocalName())) {
            Node n = elt.getChildNodes().item(0);
            if(n == null || Boolean.parseBoolean(n.getNodeValue())) {
                //We need to register a bean processor to add annotation-based config
                if(!!!cdr.containsComponentDefinition(Constants.ANNOTATION_PARSER_BEAN_NAME)) {
                	
                	MutablePassThroughMetadata mptmd = pc.createMetadata(MutablePassThroughMetadata.class);
                	mptmd.setId(Constants.ANNOTATION_PARSER_BEAN_NAME);
                	mptmd.setObject(new AnnotationParser(cdr, interceptor, metaDataHelper));
                    cdr.registerComponentDefinition(mptmd);
                }
            }
        }
        
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("parser done with " + elt);
    }

    private TransactionPropagationType getType(String typeSt) {
        return typeSt == null || typeSt.length() == 0 ? null : TransactionPropagationType.valueOf(typeSt);
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
    	if (Constants.TRANSACTION10URI.equals(arg0)) {
    	    return this.getClass().getResource(Constants.TX10_SCHEMA);
    	} else if (Constants.TRANSACTION11URI.equals(arg0)) {
    	    return this.getClass().getResource(Constants.TX11_SCHEMA);
    	} else if (Constants.TRANSACTION12URI.equals(arg0)) {
    		return this.getClass().getResource(Constants.TX12_SCHEMA);
    	}
    	return null;
    }

    public final void setTxMetaDataHelper(TxComponentMetaDataHelper transactionEnhancer)
    {
        this.metaDataHelper = transactionEnhancer;
    }

    public final void setBlueprintContainer(BlueprintContainer container) 
    {
        String id = DEFAULT_INTERCEPTOR_ID;
        InputStream is = TxElementHandler.class.getResourceAsStream("/provider.properties");
        
        if (is != null) {
            try {
                Properties props = new Properties();
                props.load(is);
                if (props.containsKey(INTERCEPTOR_BLUEPRINT_ID)) {
                    id = props.getProperty(INTERCEPTOR_BLUEPRINT_ID);
                }
            } catch (IOException e) {
                LOGGER.error(Constants.MESSAGES.getMessage("unable.to.load.provider.props"), e);
            } finally {
                try {
                    is.close();
                } catch (IOException e2) {
                    LOGGER.error(Constants.MESSAGES.getMessage("exception.closing.stream"), e2);
                }
            }
        }
        
        this.interceptor = (Interceptor) container.getComponentInstance(id);
    }
    
    public Set<Class> getManagedClasses()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public boolean isRegistered(ComponentDefinitionRegistry cdr) {
        return registered.containsKey(cdr);
    }
    
    public void unregister(Bundle blueprintBundle) {
        Iterator<Map.Entry<ComponentDefinitionRegistry, Bundle>> it = registered.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ComponentDefinitionRegistry, Bundle> e = it.next();
            if (blueprintBundle.equals(e.getValue())) {
                metaDataHelper.unregister(e.getKey());
                it.remove();
            }
        }
    }
    
    private void registerComponentsWithInterceptor(ComponentDefinitionRegistry cdr, String bean) {        
        Set<String> ids = cdr.getComponentDefinitionNames();

        if (bean == null || bean.length() == 0) {
            // in this case, let's attempt to register all components
            // if the component has already been registered with this interceptor,
            // the registration will be ignored.
            for (String id : ids) {
                ComponentMetadata componentMetadata = cdr.getComponentDefinition(id);
                cdr.registerInterceptorWithComponent(componentMetadata, interceptor);
            }
        } else {
            //create a dummy bundle wide tx data, so we can get the bean patterns from it
            BundleWideTxData data = new BundleWideTxData(null, "*", bean);
            for (Pattern p : data.getBean()) {
              for (String id : ids) {
                  Matcher m = p.matcher(id);
                  if (m.matches()) {
                      ComponentMetadata componentMetadata = cdr.getComponentDefinition(id);
                      cdr.registerInterceptorWithComponent(componentMetadata, interceptor);
                  }
              }
            }
        }
    }
}
