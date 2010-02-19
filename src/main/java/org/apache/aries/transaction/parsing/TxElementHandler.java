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
import java.util.Set;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.transaction.TxComponentMetaDataHelper;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TxElementHandler implements NamespaceHandler {
    private static final Logger _logger =
        LoggerFactory.getLogger("org.apache.aries.transaction.parsing");

    private TxComponentMetaDataHelper metaDataHelper;
    private Interceptor interceptor = null;

    private void parseElement(Element elt, ComponentMetadata cm, ParserContext pc)
    {
        if (_logger.isDebugEnabled())
            _logger.debug("parser asked to parse .. " + elt);

        if ("transaction".equals(elt.getLocalName())) {
            if (_logger.isDebugEnabled())
                _logger.debug("parser adding interceptor for " + elt);

            ComponentDefinitionRegistry cdr = pc.getComponentDefinitionRegistry();
            cdr.registerInterceptorWithComponent(cm, interceptor);
            if (_logger.isDebugEnabled())
                _logger.debug("parser setting comp trans data for " + elt.getAttribute("value") + "  "
                        + elt.getAttribute("method"));

            metaDataHelper.setComponentTransactionData(cm, elt.getAttribute("value"), elt
                    .getAttribute("method"));
        }
        
        if (_logger.isDebugEnabled())
            _logger.debug("parser done with " + elt);
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
        //really not sure here if using enclosing component is valid...
        //TODO: confirm if null may be better.
        parseElement(elt, pc.getEnclosingComponent(), pc);
        return null;
    }

    public URL getSchemaLocation(String arg0)
    {
        return this.getClass().getResource("transaction.xsd");
    }

    public final void setTxMetaDataHelper(TxComponentMetaDataHelper transactionEnhancer)
    {
        this.metaDataHelper = transactionEnhancer;
    }

    public final void setTransactionInterceptor(Interceptor itx)
    {
        if (_logger.isDebugEnabled())
            _logger.debug("parser having interceptor set " + itx);
        
        this.interceptor = itx;
    }

    public Set<Class> getManagedClasses()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
