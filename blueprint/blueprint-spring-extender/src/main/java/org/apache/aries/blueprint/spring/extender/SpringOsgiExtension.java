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
package org.apache.aries.blueprint.spring.extender;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.services.BlueprintExtenderService;
import org.apache.felix.utils.extender.Extension;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class SpringOsgiExtension implements Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringOsgiExtension.class);

    private final BlueprintExtenderService blueprintExtenderService;
    private final Bundle bundle;
    private final List<URL> paths;

    BlueprintContainer container;

    public SpringOsgiExtension(BlueprintExtenderService blueprintExtenderService, Bundle bundle, List<URL> paths) {
        // TODO: parse Spring-Context header directives
        // TODO:   create-asynchrously
        // TODO:   wait-for-dependencies
        // TODO:   timeout
        // TODO:   publish-context
        this.blueprintExtenderService = blueprintExtenderService;
        this.bundle = bundle;
        this.paths = paths;
    }

    @Override
    public void start() throws Exception {
        List<Object> bpPaths = new ArrayList<Object>();

        Set<URI> namespaces = new LinkedHashSet<URI>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        for (URL url : paths) {
            InputStream is = url.openStream();
            try {
                InputSource inputSource = new InputSource(is);
                DocumentBuilder builder = dbf.newDocumentBuilder();
                Document doc = builder.parse(inputSource);
                Attr schemaLoc = doc.getDocumentElement().getAttributeNodeNS("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
                if (schemaLoc != null) {
                    List<String> locs = new ArrayList<String>(Arrays.asList(schemaLoc.getValue().split("\\s+")));
                    locs.remove("");
                    for (int i = 0; i < locs.size() / 2; i++) {
                        String ns = locs.get(i * 2);
                        namespaces.add(URI.create(ns));
                        if (ns.startsWith("http://www.springframework.org/schema/osgi-compendium")) {
                            namespaces.add(URI.create(SpringOsgiCompendiumNamespaceHandler.CM_NAMESPACE));
                        }
                    }
                }
            } finally {
                is.close();
            }
        }

        File file = File.createTempFile("blueprint-spring-extender", ".xml");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        try {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<blueprint xmlns=\"http://www.osgi.org/xmlns/blueprint/v1.0.0\"\n");
            writer.write("\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            writer.write("\txmlns:bean=\"http://www.springframework.org/schema/beans\"\n");
            writer.write("\txsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.2.xsd\">\n");
            for (URL url : paths) {
                writer.write("\t<bean:import resource=\"" + url.toString() + "\"/>\n");
            }
            writer.write("</blueprint>\n");
        } finally {
            writer.close();
        }
        LOGGER.info("Generated blueprint for bundle {}/{} at {}", bundle.getSymbolicName(), bundle.getVersion(), file);
        bpPaths.add(file.toURI().toURL());
        container = blueprintExtenderService.createContainer(bundle, bpPaths, namespaces);
    }

    @Override
    public void destroy() throws Exception {
        // Make sure the container has not been destroyed yet
        if (container == blueprintExtenderService.getContainer(bundle)) {
            blueprintExtenderService.destroyContainer(bundle, container);
        }
    }

}
