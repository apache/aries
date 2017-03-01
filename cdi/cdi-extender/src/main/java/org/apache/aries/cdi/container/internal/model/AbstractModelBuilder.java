package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.util.Reflection.cast;
import static org.osgi.service.cdi.CdiExtenderConstants.REQUIREMENT_BEANS_ATTRIBUTE;
import static org.osgi.service.cdi.CdiExtenderConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
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
		Map<String, Object> attributes = getAttributes();

		List<String> beanDescriptorPaths = cast(attributes.get(REQUIREMENT_BEANS_ATTRIBUTE));

		if (beanDescriptorPaths != null) {
			for (String descriptorPath : beanDescriptorPaths) {
				Collection<String> resources = getResources(descriptorPath);

				if (resources != null) {
					for (String resource : resources) {
						URL url = getResource(resource);

						if (url != null) {
							beanDescriptorURLs.add(url);
						}
					}
				}
			}
		}

		String osgiBeansDescriptorPath = cast(attributes.get(REQUIREMENT_OSGI_BEANS_ATTRIBUTE));

		if (osgiBeansDescriptorPath == null) {
			osgiBeansDescriptorPath = "OSGI-INF/cdi/osgi-beans.xml";
		}

		URL osgiBeansDescriptorURL = getResource(osgiBeansDescriptorPath);

		return parse(osgiBeansDescriptorURL, beanDescriptorURLs);
	}

	abstract Map<String, Object> getAttributes();

	abstract ClassLoader getClassLoader();

	abstract URL getResource(String resource);

	abstract Collection<String> getResources(String descriptorString);

	private OSGiBeansHandler getHandler(List<URL> beanDescriptorURLs) {
		return new OSGiBeansHandler(beanDescriptorURLs);
	}

	private BeansModel parse(URL osgiBeansDescriptorURL, List<URL> beanDescriptorURLs) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);

		if (osgiBeansDescriptorURL == null) {
			throw new IllegalArgumentException("Missing osgi-beans descriptor: " + osgiBeansDescriptorURL);
		}

		SAXParser parser;

		try {
			parser = factory.newSAXParser();
		}
		catch (ParserConfigurationException | SAXException e) {
			return Throw.exception(e);
		}

		InputStream inputStream = null;

		try {
			inputStream = osgiBeansDescriptorURL.openStream();
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

			OSGiBeansHandler handler = getHandler(beanDescriptorURLs);

			parser.parse(source, handler);

			return handler.createBeansModel();
		}
		catch (IOException | SAXException e) {
			return Throw.exception(e);
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
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
