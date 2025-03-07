/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin;

import org.apache.aries.blueprint.plugin.model.Blueprint;
import org.apache.aries.blueprint.plugin.test.transactionenable.TxBean;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xbean.finder.ClassFinder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.aries.blueprint.plugin.FilteredClassFinder.findClasses;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EnableAnnotationTest {

    private static final String NS_JPA = "http://aries.apache.org/xmlns/jpa/v1.1.0";
    private static final String NS_TX1_0 = "http://aries.apache.org/xmlns/transactions/v1.0.0";
    private static final String NS_TX1_1 = "http://aries.apache.org/xmlns/transactions/v1.1.0";
    private static final String NS_TX1_2 = "http://aries.apache.org/xmlns/transactions/v1.2.0";

    private static Set<Class<?>> beanClasses;

    private XPath xpath;
    private Document document;
    private byte[] xmlAsBytes;

    @BeforeClass
    public static void setUp() throws Exception {
        ClassFinder classFinder = new ClassFinder(EnableAnnotationTest.class.getClassLoader());
        beanClasses = findClasses(classFinder, Arrays.asList(
                TxBean.class.getPackage().getName()));
    }

    private void writeXML(String namespace, String enableAnnotations) throws XMLStreamException,
            UnsupportedEncodingException, ParserConfigurationException, SAXException, IOException {
        Set<String> namespaces = new HashSet<>(Arrays.asList(NS_JPA, namespace));
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put("transaction.enableAnnotation", enableAnnotations);
        BlueprintConfigurationImpl blueprintConfiguration = new BlueprintConfigurationImpl(namespaces, null, customParameters, null, null);
        Blueprint blueprint = new Blueprint(blueprintConfiguration, beanClasses);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new BlueprintFileWriter(os).write(blueprint);

        xmlAsBytes = os.toByteArray();
        System.out.println(new String(xmlAsBytes, "UTF-8"));

        document = readToDocument(xmlAsBytes, false);
        xpath = XPathFactory.newInstance().newXPath();
    }

    @Test
    public void testNS1_0() throws Exception {
        writeXML(NS_TX1_0, "true");
        assertNull(getEnableAnnotationTx1());
    }

    @Test
    public void testNS1_1() throws Exception {
        writeXML(NS_TX1_1, "true");
        assertNull(getEnableAnnotationTx1());
    }

    @Test
    public void testNS1_2_enabled() throws Exception {
        writeXML(NS_TX1_2, "true");
        assertNotNull(getEnableAnnotationTx1());
    }

    @Test
    public void testNS1_2_disabled() throws Exception {
        writeXML(NS_TX1_2, "false");
        assertNull(getEnableAnnotationTx1());
    }

    @Test
    public void testNS1_2_default() throws Exception {
        writeXML(NS_TX1_2, null);
        assertNotNull(getEnableAnnotationTx1());
    }

    private Document readToDocument(byte[] xmlAsBytes, boolean nameSpaceAware)
            throws ParserConfigurationException,
            SAXException, IOException {

        InputStream is = new ByteArrayInputStream(xmlAsBytes);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(nameSpaceAware);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        return builder.parse(is);
    }

    private Node getEnableAnnotationTx1() throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/enable-annotations", document, XPathConstants.NODE);
    }
}
