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
package org.apache.aries.blueprint.container;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.parser.Parser;
import org.apache.aries.blueprint.services.ParserService;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;

public class ParserServiceImpl implements ParserService {

	final NamespaceHandlerRegistry _namespaceHandlerRegistry;
    final boolean _ignoreUnknownNamespaceHandlers;

  public ParserServiceImpl (NamespaceHandlerRegistry nhr, boolean ignoreUnknownNamespaceHandlers) { 
    _namespaceHandlerRegistry = nhr;
    _ignoreUnknownNamespaceHandlers = ignoreUnknownNamespaceHandlers;
  }

  public ComponentDefinitionRegistry parse(URL url, Bundle clientBundle) throws Exception {
    return parse (url, clientBundle, false);
  }

  public ComponentDefinitionRegistry parse(URL url, Bundle clientBundle, boolean validate)
      throws Exception {
    List<URL> urls = new ArrayList<URL>();
    urls.add(url);
    return parse (urls, clientBundle, validate);
  }
  
  public ComponentDefinitionRegistry parse(List<URL> urls, Bundle clientBundle) throws Exception {
    return parse(urls, clientBundle, false);
  }
  
	public ComponentDefinitionRegistry parse(List<URL> urls, Bundle clientBundle, boolean validate) throws Exception {
	  Parser parser = new Parser(null, _ignoreUnknownNamespaceHandlers);   
	  parser.parse(urls);
	  return validateAndPopulate (parser, clientBundle, validate);
	}
	
  public ComponentDefinitionRegistry parse(InputStream is, Bundle clientBundle) throws Exception {
    return parse (is, clientBundle, false);
  }
  
  public ComponentDefinitionRegistry parse(InputStream is, Bundle clientBundle, boolean validate) throws Exception {
    Parser parser = new Parser(null, _ignoreUnknownNamespaceHandlers);
    parser.parse(is);
    return validateAndPopulate (parser, clientBundle, validate);
  }
    
  private ComponentDefinitionRegistry validateAndPopulate (Parser parser, Bundle clientBundle, boolean validate) 
  throws IOException, SAXException { 
    Set<URI> nsuris = parser.getNamespaces();
    ComponentDefinitionRegistry cdr;
    NamespaceHandlerSet nshandlers = _namespaceHandlerRegistry.getNamespaceHandlers(nsuris, clientBundle);
    try {
        if (validate) { 
          parser.validate( nshandlers.getSchema());
        }
        cdr = new ComponentDefinitionRegistryImpl();
        parser.populate(nshandlers, cdr);
    } finally {
        nshandlers.destroy();
    }
    
    return cdr;   
  }

}
