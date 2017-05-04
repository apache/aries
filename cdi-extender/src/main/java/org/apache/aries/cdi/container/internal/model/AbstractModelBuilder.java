/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.util.Reflection.cast;
import static org.osgi.service.cdi.CdiConstants.REQUIREMENT_BEANS_ATTRIBUTE;
import static org.osgi.service.cdi.CdiConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.aries.cdi.container.internal.util.Throw;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public abstract class AbstractModelBuilder {

	public BeansModel build() {
		List<URL> beanDescriptorURLs = new ArrayList<URL>();
		List<URL> osgiBeanDescriptorURLs = new ArrayList<URL>();
		Map<String, Object> attributes = getAttributes();

		List<String> beanDescriptorPaths = cast(attributes.get(REQUIREMENT_BEANS_ATTRIBUTE));

		if (beanDescriptorPaths != null) {
			for (String descriptorPath : beanDescriptorPaths) {
				URL url = getResource(descriptorPath);

				if (url != null) {
					beanDescriptorURLs.add(url);
				}
			}
		}

		List<String> osgiBeansDescriptorPaths = cast(attributes.get(REQUIREMENT_OSGI_BEANS_ATTRIBUTE));

		if (osgiBeansDescriptorPaths == null) {
			osgiBeansDescriptorPaths = getDefaultResources();
		}

		if (osgiBeansDescriptorPaths != null) {
			for (String descriptorPath : osgiBeansDescriptorPaths) {
				URL url = getResource(descriptorPath);

				if (url != null) {
					osgiBeanDescriptorURLs.add(url);
				}
			}
		}

		return parse(osgiBeanDescriptorURLs, beanDescriptorURLs);
	}

	abstract Map<String, Object> getAttributes();

	abstract ClassLoader getClassLoader();

	abstract URL getResource(String resource);

	abstract List<String> getDefaultResources();

	private OSGiBeansHandler getHandler(List<URL> beanDescriptorURLs) {
		return new OSGiBeansHandler(beanDescriptorURLs);
	}

	private BeansModel parse(List<URL> osgiBeansDescriptorURLs, List<URL> beanDescriptorURLs) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);

		if (osgiBeansDescriptorURLs.isEmpty()) {
			throw new IllegalArgumentException("Missing osgi-beans descriptors");
		}

		SAXParser parser;

		try {
			parser = factory.newSAXParser();
		}
		catch (ParserConfigurationException | SAXException e) {
			return Throw.exception(e);
		}

		OSGiBeansHandler handler = getHandler(beanDescriptorURLs);

		for (URL osgiBeansDescriptorURL: osgiBeansDescriptorURLs) {
			try (InputStream inputStream = osgiBeansDescriptorURL.openStream()) {
				InputSource source = new InputSource(inputStream);

				if (source.getByteStream().available() == 0) {
					throw new IllegalArgumentException(
						"Specified osgi-beans descriptor is empty: " + osgiBeansDescriptorURL);
				}

				try {
					parser.setProperty(
						"http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
					parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", loadXsds());
				}
				catch (IllegalArgumentException | SAXNotRecognizedException | SAXNotSupportedException e) {
					// No op, we just don't validate the XML
				}

				parser.parse(source, handler);
			}
			catch (IOException | SAXException e) {
				return Throw.exception(e);
			}
		}

		return handler.createBeansModel();
	}

	private InputSource loadXsd(String name) {
		InputStream in = getClassLoader().getResourceAsStream(name);
		if (in == null) {
			return null;
		}
		else {
			return new InputSource(in);
		}
	}

	private InputSource[] loadXsds() {
		List<InputSource> xsds = new ArrayList<InputSource>();

		for (XmlSchema schema : XmlSchema.values()) {
			InputSource source = loadXsd(schema.getFileName());
			if (source != null) {
				xsds.add(source);
			}
		}

		return xsds.toArray(new InputSource[0]);
	}

}
