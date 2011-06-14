package org.apache.aries.application.modelling.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.validation.Schema;

import org.apache.aries.application.modelling.ModelledResourceManager;
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
		private final Parser parser = new Parser();
		
		protected ComponentDefinitionRegistry parseCDR(List<URL> blueprintsToParse) throws Exception {
			parser.parse(blueprintsToParse);
			return getCDR();
		}
			
		protected ComponentDefinitionRegistry parseCDR(InputStream blueprintToParse) throws Exception {
			parser.parse(blueprintToParse);
			return getCDR();
		}
		
		private ComponentDefinitionRegistry getCDR() {
			ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
			parser.populate(DUMMY_HANDLER_SET, cdr);
			return cdr;			
		}
	};
	
	public static ModelledResourceManager getModelledResourceManager() {
		ModellingManagerImpl modellingManager = new ModellingManagerImpl();
		
		OfflineParserProxy parserProxy = new OfflineParserProxy();
		parserProxy.setModellingManager(modellingManager);
		
		ModelledResourceManagerImpl result = new ModelledResourceManagerImpl();
		result.setModellingManager(modellingManager);
		result.setParserProxy(parserProxy);
		
		return result;
	}
}
