package org.apache.aries.blueprint.container;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ParserService;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry.NamespaceHandlerSet;
import org.apache.aries.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;

public class ParserServiceImpl implements ParserService {

	NamespaceHandlerRegistry _namespaceHandlerRegistry;
	
	public ParserServiceImpl (NamespaceHandlerRegistry nhr) { 
		_namespaceHandlerRegistry = nhr;
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
	  Parser parser = new Parser();   
	  parser.parse(urls);
	  return validateAndPopulate (parser, clientBundle, validate);
	}
	
  public ComponentDefinitionRegistry parse(InputStream is, Bundle clientBundle) throws Exception {
    return parse (is, clientBundle, false);
  }
  
  public ComponentDefinitionRegistry parse(InputStream is, Bundle clientBundle, boolean validate) throws Exception {
    Parser parser = new Parser();
    parser.parse(is);
    return validateAndPopulate (parser, clientBundle, validate);
  }
    
  private ComponentDefinitionRegistry validateAndPopulate (Parser parser, Bundle clientBundle, boolean validate) 
  throws IOException, SAXException { 
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
