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

import com.google.common.collect.Sets;
import org.apache.aries.blueprint.plugin.model.Context;
import org.apache.aries.blueprint.plugin.model.TransactionalDef;
import org.apache.aries.blueprint.plugin.test.MyBean1;
import org.apache.aries.blueprint.plugin.test.MyProduced;
import org.apache.aries.blueprint.plugin.test.ServiceA;
import org.apache.aries.blueprint.plugin.test.ServiceB;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xbean.finder.ClassFinder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.aries.blueprint.plugin.FilteredClassFinder.findClasses;
import static org.junit.Assert.assertEquals;

public class GeneratorTest {

    private static XPath xpath;
    private static Document document;

    @BeforeClass
    public static void setUp() throws Exception {
        ClassFinder classFinder = new ClassFinder(GeneratorTest.class.getClassLoader());
        String packageName = MyBean1.class.getPackage().getName();
        Set<Class<?>> beanClasses = findClasses(classFinder, Collections.singletonList(packageName));
        Context context = new Context(beanClasses);
        context.resolve();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Set<String> namespaces = new HashSet<String>(Arrays.asList(Generator.NS_JPA, Generator.NS_TX));
        new Generator(context, os, namespaces).generate();
        System.out.println(os.toString("UTF-8"));

        document = readToDocument(os);
        xpath = XPathFactory.newInstance().newXPath();
    }

    @Test
    public void testGenerateBeanWithInitDestroyAndfieldInjection() throws Exception {
        Node bean1 = getBeanById("myBean1");

        assertEquals(MyBean1.class.getName(), xpath.evaluate("@class", bean1));
        assertEquals("init", xpath.evaluate("@init-method", bean1));
        assertEquals("destroy", xpath.evaluate("@destroy-method", bean1));
        assertEquals("true", xpath.evaluate("@field-injection", bean1));
        assertEquals("", xpath.evaluate("@scope", bean1));
    }

    @Test
    public void testGenerateTransactional() throws Exception {
        Node bean1 = getBeanById("myBean1");

        NodeList txs = (NodeList) xpath.evaluate("transaction", bean1, XPathConstants.NODESET);
        Set<TransactionalDef> defs = new HashSet<TransactionalDef>();
        for (int i = 0; i < txs.getLength(); ++i) {
            Node tx = txs.item(i);
            defs.add(new TransactionalDef(xpath.evaluate("@method", tx), xpath.evaluate("@value", tx)));
        }
        Set<TransactionalDef> expectedDefs = Sets.newHashSet(new TransactionalDef("*", "RequiresNew"),
            new TransactionalDef("txNotSupported", "NotSupported"),
            new TransactionalDef("txMandatory", "Mandatory"),
            new TransactionalDef("txNever", "Never"),
            new TransactionalDef("txRequired", "Required"),
            new TransactionalDef("txOverridenWithRequiresNew", "RequiresNew"),
            new TransactionalDef("txSupports", "Supports"));
        assertEquals(expectedDefs, defs);
    }

    @Test
    public void testGeneratePersistenceContext() throws Exception {
        Node bean1 = getBeanById("myBean1");

        assertEquals("person", xpath.evaluate("context/@unitname", bean1));
        assertEquals("em", xpath.evaluate("context/@property", bean1));
    }

    @Test
    public void testGeneratePersistenceUnit() throws Exception {
        Node bean1 = getBeanById("myBean1");

        assertEquals("person", xpath.evaluate("unit/@unitname", bean1));
        assertEquals("emf", xpath.evaluate("unit/@property", bean1));
    }

    @Test
    public void testGenerateAutowiredBean() throws Exception {
        Node bean1 = getBeanById("myBean1");

        assertEquals("my1", xpath.evaluate("property[@name='bean2']/@ref", bean1));
    }

    @Test
    public void testGenerateServiceWithOneInterface() throws Exception {
        Node serviceAImpl2 = getServiceByRef("my2");
        assertEquals(ServiceA.class.getName(), xpath.evaluate("@interface", serviceAImpl2));
        assertEquals("", xpath.evaluate("@auto-export", serviceAImpl2));
        assertEquals("", xpath.evaluate("interfaces", serviceAImpl2));
    }

    @Test
    public void testGenerateServiceWithAutoExport() throws Exception {
        Node serviceAImpl3 = getServiceByRef("serviceAImpl3");
        assertEquals("", xpath.evaluate("@interface", serviceAImpl3));
        assertEquals("interfaces", xpath.evaluate("@auto-export", serviceAImpl3));
        assertEquals("", xpath.evaluate("interfaces", serviceAImpl3));
    }

    @Test
    public void testGenerateServiceWith2Interfaces() throws Exception {
        Node serviceABImpl = getServiceByRef("serviceABImpl");
        assertEquals("", xpath.evaluate("@interface", serviceABImpl));
        assertEquals("", xpath.evaluate("@auto-export", serviceABImpl));

        NodeList interfaceValues = (NodeList) xpath.evaluate("interfaces/value", serviceABImpl, XPathConstants.NODESET);
        Set<String> interfaceNames = new HashSet<String>();
        for (int i = 0; i < interfaceValues.getLength(); ++i) {
            Node interfaceValue = interfaceValues.item(i);
            interfaceNames.add(interfaceValue.getTextContent());
        }
        assertEquals(Sets.newHashSet(ServiceA.class.getName(), ServiceB.class.getName()),
            interfaceNames);
    }

    @Test
    public void testGenerateBeanWithConstructorInjection() throws Exception {
        // Bean with constructor injection
        Node myBean5 = getBeanById("myBean5");
        assertEquals("my2", xpath.evaluate("argument[1]/@ref", myBean5));
        assertEquals("my1", xpath.evaluate("argument[2]/@ref", myBean5));
        assertEquals("serviceABImpl", xpath.evaluate("argument[3]/@ref", myBean5));
        assertEquals("100", xpath.evaluate("argument[4]/@value", myBean5));
        assertEquals("ser1", xpath.evaluate("argument[5]/@ref", myBean5));
        assertEquals("ser2", xpath.evaluate("argument[6]/@ref", myBean5));
        assertEquals("serviceAImplQualified", xpath.evaluate("argument[7]/@ref", myBean5));
    }

    @Test
    public void testGenerateBeanWithConstructorInjectionWithoutInjectAnnotation() throws Exception {
        // Bean with constructor injection
        Node myBean6 = getBeanById("myBean6");
        assertEquals("my2", xpath.evaluate("argument[1]/@ref", myBean6));
    }

    @Test
    public void testGenerateReferenceWithComponentName() throws Exception {
        Node ser1 = getReferenceById("ser1");
        assertEquals("myRef", xpath.evaluate("@component-name", ser1));
        assertEquals("", xpath.evaluate("@filter", ser1));
    }

    @Test
    public void testGenerateReferenceWithFilter() throws Exception {
        Node ser2 = getReferenceById("ser2");
        assertEquals("", xpath.evaluate("@component-name", ser2));
        assertEquals("(mode=123)", xpath.evaluate("@filter", ser2));
    }

    @Test
    public void testProducesNamedBeans() throws Exception {
        Node bean1 = getBeanById("produced1");
        assertEquals("org.apache.aries.blueprint.plugin.test.MyProduced", xpath.evaluate("@class", bean1));
        assertEquals("myFactoryNamedBean", xpath.evaluate("@factory-ref", bean1));
        assertEquals("createBean1", xpath.evaluate("@factory-method", bean1));
        assertEquals("prototype", xpath.evaluate("@scope", bean1));

        Node bean2 = getBeanById("produced2");
        assertEquals("org.apache.aries.blueprint.plugin.test.MyProduced", xpath.evaluate("@class", bean1));
        assertEquals("myFactoryNamedBean", xpath.evaluate("@factory-ref", bean2));
        assertEquals("createBean2", xpath.evaluate("@factory-method", bean2));
        assertEquals("", xpath.evaluate("@scope", bean2));

        Node myBean5 = getBeanById("myBean5");
        assertEquals("produced2", xpath.evaluate("argument[8]/@ref", myBean5));
    }

    @Test
    public void testProducesBeanUsingParametersNotConstructor() throws Exception {
        Node bean1 = getBeanById("myProducedWithConstructor");
        assertEquals("org.apache.aries.blueprint.plugin.test.MyProducedWithConstructor", xpath.evaluate("@class", bean1));
        assertEquals("myFactoryBean", xpath.evaluate("@factory-ref", bean1));
        assertEquals("createBeanWithParameters", xpath.evaluate("@factory-method", bean1));
        assertEquals("myBean1", xpath.evaluate("argument[1]/@ref", bean1));
        assertEquals("100", xpath.evaluate("argument[2]/@value", bean1));
        assertEquals("ser1", xpath.evaluate("argument[3]/@ref", bean1));
    }

    @Test
    public void testExposeProducedBeanAsServiceWithAutoExport() throws Exception {
        Node service = getServiceByRef("producedForService");
        assertEquals("interfaces", xpath.evaluate("@auto-export", service));
        assertEquals("", xpath.evaluate("@interface", service));
        assertEquals("0", xpath.evaluate("count(interfaces)", service));
        assertEquals("0", xpath.evaluate("count(service-properties)", service));
    }

    @Test
    public void testExposeProducedBeanAsServiceWithOneInterface() throws Exception {
        Node service = getServiceByRef("producedForServiceWithOneInterface");
        assertEquals("", xpath.evaluate("@auto-export", service));
        assertEquals(MyProduced.class.getName(), xpath.evaluate("@interface", service));
        assertEquals("0", xpath.evaluate("count(interfaces)", service));
        assertEquals("0", xpath.evaluate("count(service-properties)", service));
    }

    @Test
    public void testExposeProducedBeanAsServiceWithTwoInterfaces() throws Exception {
        Node service = getServiceByRef("producedForServiceWithTwoInterfaces");
        assertEquals("", xpath.evaluate("@auto-export", service));
        assertEquals("", xpath.evaluate("@interface", service));
        assertEquals("2", xpath.evaluate("count(interfaces/value)", service));
        assertEquals(MyProduced.class.getName(), xpath.evaluate("interfaces/value[1]", service));
        assertEquals(ServiceA.class.getName(), xpath.evaluate("interfaces/value[2]", service));
        assertEquals("0", xpath.evaluate("count(service-properties)", service));
    }

    @Test
    public void testExposeProducedBeanAsServiceWithServiceProperties() throws Exception {
        Node service = getServiceByRef("producedForServiceWithProperties");
        assertEquals("interfaces", xpath.evaluate("@auto-export", service));
        assertEquals("", xpath.evaluate("@interface", service));
        assertEquals("0", xpath.evaluate("count(interfaces)", service));
        assertEquals("2", xpath.evaluate("count(service-properties/entry)", service));
        assertEquals("v1", xpath.evaluate("service-properties/entry[@key='n1']/@value", service));
        assertEquals("v2", xpath.evaluate("service-properties/entry[@key='n2']/@value", service));
    }

    @Test
    public void testSetterInjection() throws Exception {
        Node bean1 = getBeanById("beanWithSetters");

        assertEquals("0", xpath.evaluate("count(property[@name='useless'])", bean1));
        assertEquals("0", xpath.evaluate("count(property[@name='iOnlyHaveSetPrefix'])", bean1));
        assertEquals("0", xpath.evaluate("count(property[@name='ihaveMoreThenOneParameter'])", bean1));
        assertEquals("0", xpath.evaluate("count(property[@name='iOnlyHaveSetPrefixValue'])", bean1));
        assertEquals("0", xpath.evaluate("count(property[@name='ihaveMoreThenOneParameterValue'])", bean1));

        assertEquals("test", xpath.evaluate("property[@name='myValue']/@value", bean1));
        assertEquals("my1", xpath.evaluate("property[@name='serviceA1']/@ref", bean1));
        assertEquals("my1", xpath.evaluate("property[@name='serviceA2']/@ref", bean1));
        assertEquals("serviceABImpl", xpath.evaluate("property[@name='serviceB']/@ref", bean1));
        assertEquals("serviceB2Id", xpath.evaluate("property[@name='serviceB2']/@ref", bean1));
        assertEquals("serviceB-typeB1Ref", xpath.evaluate("property[@name='serviceBRef']/@ref", bean1));
        assertEquals("serviceB2IdRef", xpath.evaluate("property[@name='serviceB2Ref']/@ref", bean1));
        assertEquals("serviceB-B3Ref", xpath.evaluate("property[@name='serviceB3Ref']/@ref", bean1));

        Node reference1 = getReferenceById("serviceB-typeB1Ref");
        assertEquals(ServiceB.class.getName(), xpath.evaluate("@interface", reference1));
        assertEquals("(type=B1Ref)", xpath.evaluate("@filter", reference1));

        Node reference2 = getReferenceById("serviceB2IdRef");
        assertEquals(ServiceB.class.getName(), xpath.evaluate("@interface", reference2));
        assertEquals("(type=B2Ref)", xpath.evaluate("@filter", reference2));

        Node reference3 = getReferenceById("serviceB-B3Ref");
        assertEquals(ServiceB.class.getName(), xpath.evaluate("@interface", reference3));
        assertEquals("B3Ref", xpath.evaluate("@component-name", reference3));
    }

    private static Document readToDocument(ByteArrayOutputStream os) throws ParserConfigurationException,
        SAXException, IOException {
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        return builder.parse(is);
    }

    private static Node getBeanById(String id) throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/bean[@id='" + id + "']", document, XPathConstants.NODE);
    }

    private static Node getServiceByRef(String id) throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/service[@ref='" + id + "']", document, XPathConstants.NODE);
    }

    private static Node getReferenceById(String id) throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/reference[@id='" + id + "']", document, XPathConstants.NODE);
    }

}
