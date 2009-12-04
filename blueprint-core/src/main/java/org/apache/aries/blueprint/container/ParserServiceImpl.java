package org.apache.aries.blueprint.container;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ParserService;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry.NamespaceHandlerSet;
import org.apache.aries.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.osgi.framework.Bundle;

public class ParserServiceImpl implements ParserService {

	NamespaceHandlerRegistry _namespaceHandlerRegistry;
	
	public ParserServiceImpl (NamespaceHandlerRegistry nhr) { 
		_namespaceHandlerRegistry = nhr;
	}
	public ComponentDefinitionRegistry parse(List<URL> urls, Bundle clientBundle) throws Exception {
		return parse(urls, clientBundle, false);
	}
	
	public ComponentDefinitionRegistry parse(List<URL> urls, Bundle clientBundle, boolean validate) throws Exception {
		Parser parser = new Parser();   
		parser.parse(urls);
		Set<URI> nsuris = parser.getNamespaces();
		NamespaceHandlerSet nshandlers = _namespaceHandlerRegistry.getNamespaceHandlers(nsuris, clientBundle);
		if (validate) { 
		  parser.validate( nshandlers.getSchema());
		}
		ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
		parser.populate(nshandlers, cdr);
		return cdr;		
	}
}
