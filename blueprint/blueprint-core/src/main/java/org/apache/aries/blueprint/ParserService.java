package org.apache.aries.blueprint;

import java.net.URL;
import java.util.List;

import org.osgi.framework.Bundle;

public interface ParserService {

	ComponentDefinitionRegistry parse (List<URL> urls, Bundle clientBundle) throws Exception;
	
	ComponentDefinitionRegistry parse (List<URL> urls, Bundle clientBundle, boolean validate) throws Exception;
	
}
