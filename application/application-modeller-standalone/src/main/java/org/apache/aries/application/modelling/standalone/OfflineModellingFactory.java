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
package org.apache.aries.application.modelling.standalone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.xml.validation.Schema;

import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ParserProxy;
import org.apache.aries.application.modelling.ServiceModeller;
import org.apache.aries.application.modelling.impl.AbstractParserProxy;
import org.apache.aries.application.modelling.impl.ModelledResourceManagerImpl;
import org.apache.aries.application.modelling.impl.ModellingManagerImpl;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.parser.Parser;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class OfflineModellingFactory {
	private static final NamespaceHandlerSet DUMMY_HANDLER_SET = new NamespaceHandlerSet() {
		public NamespaceHandler getNamespaceHandler(URI arg0) {
			return new NamespaceHandler() {
				
				public Metadata parse(Element arg0, ParserContext arg1) {
					return NullMetadata.NULL;
				}
				
				public URL getSchemaLocation(String arg0) {
					return null;
				}
				
				public Set<Class> getManagedClasses() {
					return Collections.emptySet();
				}
				
				public ComponentMetadata decorate(Node arg0, ComponentMetadata arg1, ParserContext arg2) {
					return arg1;
				}
			};
		}

		public Set<URI> getNamespaces() {
			return Collections.emptySet();
		}

		public Schema getSchema() throws SAXException, IOException {
			return null;
		}

		public boolean isComplete() {
			return true;
		}

		public void addListener(Listener arg0) {}
		public void removeListener(Listener arg0) {}
		public void destroy() {}
	};
	
	
	private static class OfflineParserProxy extends AbstractParserProxy {
		protected ComponentDefinitionRegistry parseCDR(List<URL> blueprintsToParse) throws Exception {
			Parser parser = new Parser();
			parser.parse(blueprintsToParse);
			return getCDR(parser);
		}
			
		protected ComponentDefinitionRegistry parseCDR(InputStream blueprintToParse) throws Exception {
			Parser parser = new Parser();
			parser.parse(blueprintToParse);
			return getCDR(parser);
		}
		
		private ComponentDefinitionRegistry getCDR(Parser parser) {
			ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
			parser.populate(DUMMY_HANDLER_SET, cdr);
			return cdr;			
		}
	};
	
	public static ParserProxy getOfflineParserProxy() {
		ModellingManagerImpl modellingManager = new ModellingManagerImpl();
		
		OfflineParserProxy parserProxy = new OfflineParserProxy();
		parserProxy.setModellingManager(modellingManager);
		
		return parserProxy;
	}
	
	public static ModelledResourceManager getModelledResourceManager() {
		ModellingManagerImpl modellingManager = new ModellingManagerImpl();
		
		OfflineParserProxy parserProxy = new OfflineParserProxy();
		parserProxy.setModellingManager(modellingManager);
		
		ModelledResourceManagerImpl result = new ModelledResourceManagerImpl();
		result.setModellingManager(modellingManager);
		result.setParserProxy(parserProxy);
		
		List<ServiceModeller> plugins = new ArrayList<ServiceModeller>();
		
    ClassLoader cl = OfflineModellingFactory.class.getClassLoader();
		try {
      Enumeration<URL> e = cl.getResources(
          "META-INF/services/" + ServiceModeller.class.getName());
      
      while(e.hasMoreElements()) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                e.nextElement().openStream()));
        try {
          plugins.add((ServiceModeller) Class.forName(reader.readLine(), true, cl).newInstance());
        } catch (Exception e1) {
          e1.printStackTrace(System.err);
        }
      }
    } catch (IOException e) {
      e.printStackTrace(System.err);
    }

    result.setModellingPlugins(plugins);
		return result;
	}
}
