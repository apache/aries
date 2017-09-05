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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public abstract class AbstractModelBuilder {

	static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
	static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

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

	public abstract Map<String, Object> getAttributes();

	public abstract ClassLoader getClassLoader();

	public abstract URL getResource(String resource);

	public abstract List<String> getDefaultResources();

	private OSGiBeansHandler getHandler(List<URL> beanDescriptorURLs) {
		return new OSGiBeansHandler(beanDescriptorURLs, getClassLoader());
	}

	private BeansModel parse(List<URL> osgiBeansDescriptorURLs, List<URL> beanDescriptorURLs) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(true);
		factory.setNamespaceAware(true);

		if (osgiBeansDescriptorURLs.isEmpty()) {
			throw new IllegalArgumentException("Missing osgi-beans descriptors");
		}

		SAXParser parser;

		try {
			parser = factory.newSAXParser();

			try {
				parser.setProperty(SCHEMA_LANGUAGE, XML_SCHEMA);
				parser.setProperty(SCHEMA_SOURCE, loadXsds());
			}
			catch (SAXNotRecognizedException | SAXNotSupportedException e) {
				// No op, we just don't validate the XML
			}
		}
		catch (ParserConfigurationException | SAXException e) {
			return Throw.exception(e);
		}

		OSGiBeansHandler handler = getHandler(beanDescriptorURLs);

		for (URL osgiBeansDescriptorURL: osgiBeansDescriptorURLs) {
			try (InputStream inputStream = osgiBeansDescriptorURL.openStream()) {
				InputSource source = new InputSource(inputStream);

				if (source.getByteStream().available() == 0) {
					_log.warn("CDIe - Ignoring {} because it contains 0 bytes", osgiBeansDescriptorURL);

					continue;
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

	private static final Logger _log = LoggerFactory.getLogger(AbstractModelBuilder.class);

}
