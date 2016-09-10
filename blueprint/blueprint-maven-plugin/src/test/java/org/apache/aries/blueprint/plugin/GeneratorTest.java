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

    private static final String NS_JPA = "http://aries.apache.org/xmlns/jpa/v1.1.0";
    private static final String NS_TX1 = "http://aries.apache.org/xmlns/transactions/v1.2.0";

    private static XPath xpath;
    private static Document document;

    @BeforeClass
    public static void setUp() throws Exception {
        ClassFinder classFinder = new ClassFinder(GeneratorTest.class.getClassLoader());
        String packageName = MyBean1.class.getPackage().getName();
        Set<Class<?>> beanClasses = findClasses(classFinder, Collections.singletonList(packageName));
        Set<String> namespaces = new HashSet<String>(Arrays.asList(NS_JPA, NS_TX1));
        BlueprintConfigurationImpl blueprintConfiguration = new BlueprintConfigurationImpl(namespaces, null);
        Context context = new Context(blueprintConfiguration, beanClasses);
        context.resolve();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new Generator(context, os, blueprintConfiguration).generate();
        System.out.println(os.toString("UTF-8"));

        document = readToDocument(os);
        xpath = XPathFactory.newInstance().newXPath();
    }

    @Test
    public void testGenerateBeanWithInitDestroyAndfieldInjection() throws Exception {
        Node bean1 = getBeanById("myBean1");

        assertXpathEquals(bean1, "@class", MyBean1.class.getName());
        assertXpathEquals(bean1, "@init-method", "init");
        assertXpathEquals(bean1, "@destroy-method", "destroy");
        assertXpathEquals(bean1, "@field-injection", "true");
        assertXpathDoesNotExist(bean1, "@scope");
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
    public void testGenerateCDITransactional() throws Exception {
        Node bean1 = getBeanById("cdiTransactionalAnnotatedBean");

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

        assertXpathEquals(bean1, "context/@unitname", "person");
        assertXpathEquals(bean1, "context/@property", "em");
    }

    @Test
    public void testGeneratePersistenceUnit() throws Exception {
        Node bean1 = getBeanById("myBean1");

        assertXpathEquals(bean1, "unit/@unitname", "person");
        assertXpathEquals(bean1, "unit/@property", "emf");
    }

    @Test
    public void testGenerateAutowiredBean() throws Exception {
        Node bean1 = getBeanById("myBean1");

        assertXpathEquals(bean1, "property[@name='bean2']/@ref", "my1");
    }

    @Test
    public void testGenerateServiceWithOneInterface() throws Exception {
        Node serviceAImpl2 = getServiceByRef("my2");
        assertXpathEquals(serviceAImpl2, "@interface", ServiceA.class.getName());
        assertXpathDoesNotExist(serviceAImpl2, "@auto-export");
        assertXpathDoesNotExist(serviceAImpl2, "interfaces");
    }

    @Test
    public void testGenerateServiceWithAutoExport() throws Exception {
        Node serviceAImpl3 = getServiceByRef("serviceAImpl3");
        assertXpathDoesNotExist(serviceAImpl3, "@interface");
        assertXpathEquals(serviceAImpl3, "@auto-export", "interfaces");
        assertXpathDoesNotExist(serviceAImpl3, "interfaces");
    }

    @Test
    public void testGenerateServiceWith2Interfaces() throws Exception {
        Node serviceABImpl = getServiceByRef("serviceABImpl");

        assertXpathDoesNotExist(serviceABImpl, "@interface");
        assertXpathDoesNotExist(serviceABImpl, "@auto-export");

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
        assertXpathDoesNotExist(myBean5, "@field-injection");
        assertXpathEquals(myBean5, "argument[1]/@ref", "my2");
        assertXpathEquals(myBean5, "argument[2]/@ref", "my1");
        assertXpathEquals(myBean5, "argument[3]/@ref", "serviceABImpl");
        assertXpathEquals(myBean5, "argument[4]/@value", "100");
        assertXpathEquals(myBean5, "argument[5]/@ref", "ser1");
        assertXpathEquals(myBean5, "argument[6]/@ref", "ser2");
        assertXpathEquals(myBean5, "argument[7]/@ref", "serviceAImplQualified");
    }

    @Test
    public void testGenerateBeanWithConstructorInjectionWithoutInjectAnnotation() throws Exception {
        // Bean with constructor injection
        Node myBean6 = getBeanById("myBean6");
        assertXpathEquals(myBean6, "argument[1]/@ref", "my2");
    }

    @Test
    public void testGenerateReferenceWithComponentName() throws Exception {
        Node ser1 = getReferenceById("ser1");
        assertXpathEquals(ser1, "@component-name", "myRef");
        assertXpathDoesNotExist(ser1, "@filter");
    }

    @Test
    public void testGenerateReferenceWithFilter() throws Exception {
        Node ser2 = getReferenceById("ser2");
        assertXpathDoesNotExist(ser2, "@component-name");
        assertXpathEquals(ser2, "@filter", "(mode=123)");
    }

    @Test
    public void testProducesNamedBeans() throws Exception {
        Node bean1 = getBeanById("produced1");
        assertXpathEquals(bean1, "@class", "org.apache.aries.blueprint.plugin.test.MyProduced");
        assertXpathEquals(bean1, "@factory-ref", "myFactoryNamedBean");
        assertXpathEquals(bean1, "@factory-method", "createBean1");
        assertXpathEquals(bean1, "@scope", "prototype");

        Node bean2 = getBeanById("produced2");
        assertXpathEquals(bean1, "@class", "org.apache.aries.blueprint.plugin.test.MyProduced");
        assertXpathEquals(bean2, "@factory-ref", "myFactoryNamedBean");
        assertXpathEquals(bean2, "@factory-method", "createBean2");
        assertXpathDoesNotExist(bean2, "@scope");

        Node myBean5 = getBeanById("myBean5");
        assertXpathEquals(myBean5, "argument[8]/@ref", "produced2");
    }

    @Test
    public void testProducesBeanUsingParametersNotConstructor() throws Exception {
        Node bean1 = getBeanById("myProducedWithConstructor");
        assertXpathEquals(bean1, "@class", "org.apache.aries.blueprint.plugin.test.MyProducedWithConstructor");
        assertXpathEquals(bean1, "@factory-ref", "myFactoryBean");
        assertXpathEquals(bean1, "@factory-method", "createBeanWithParameters");
        assertXpathEquals(bean1, "argument[1]/@ref", "myBean1");
        assertXpathEquals(bean1, "argument[2]/@value", "100");
        assertXpathEquals(bean1, "argument[3]/@ref", "ser1");
    }

    @Test
    public void testExposeProducedBeanAsServiceWithAutoExport() throws Exception {
        Node service = getServiceByRef("producedForService");
        assertXpathEquals(service, "@auto-export", "interfaces");
        assertXpathDoesNotExist(service, "@interface");
        assertXpathDoesNotExist(service, "interfaces");
        assertXpathDoesNotExist(service, "service-properties");
    }

    @Test
    public void testExposeProducedBeanAsServiceWithOneInterface() throws Exception {
        Node service = getServiceByRef("producedForServiceWithOneInterface");
        assertXpathDoesNotExist(service, "@auto-export");
        assertXpathEquals(service, "@interface", MyProduced.class.getName());
        assertXpathDoesNotExist(service, "interfaces");
        assertXpathDoesNotExist(service, "service-properties");
    }

    @Test
    public void testExposeProducedBeanAsServiceWithTwoInterfaces() throws Exception {
        Node service = getServiceByRef("producedForServiceWithTwoInterfaces");
        assertXpathDoesNotExist(service, "@auto-export");
        assertXpathDoesNotExist(service, "@interface");
        assertXpathEquals(service, "count(interfaces/value)", "2");
        assertXpathEquals(service, "interfaces/value[1]", MyProduced.class.getName());
        assertXpathEquals(service, "interfaces/value[2]", ServiceA.class.getName());
        assertXpathDoesNotExist(service, "service-properties");
    }

    @Test
    public void testExposeProducedBeanAsServiceWithServiceProperties() throws Exception {
        Node service = getServiceByRef("producedForServiceWithProperties");
        assertXpathEquals(service, "@auto-export", "interfaces");
        assertXpathDoesNotExist(service, "@interface");
        assertXpathDoesNotExist(service, "interfaces");
        assertXpathEquals(service, "count(service-properties/entry)", "2");
        assertXpathEquals(service, "service-properties/entry[@key='n1']/@value", "v1");
        assertXpathEquals(service, "service-properties/entry[@key='n2']/@value", "v2");
    }

    @Test
    public void testSetterInjection() throws Exception {
        Node bean1 = getBeanById("beanWithSetters");
        assertXpathDoesNotExist(bean1, "@field-injection");

        assertXpathDoesNotExist(bean1, "property[@name='useless']");
        assertXpathDoesNotExist(bean1, "property[@name='iOnlyHaveSetPrefix']");
        assertXpathDoesNotExist(bean1, "property[@name='ihaveMoreThenOneParameter']");
        assertXpathDoesNotExist(bean1, "property[@name='iOnlyHaveSetPrefixValue']");
        assertXpathDoesNotExist(bean1, "property[@name='ihaveMoreThenOneParameterValue']");

        assertXpathEquals(bean1, "property[@name='myValue']/@value", "test");
        assertXpathEquals(bean1, "property[@name='serviceA1']/@ref", "my1");
        assertXpathEquals(bean1, "property[@name='serviceA2']/@ref", "my2");
        assertXpathEquals(bean1, "property[@name='serviceB']/@ref", "serviceABImpl");
        assertXpathEquals(bean1, "property[@name='serviceB2']/@ref", "serviceB2Id");
        assertXpathEquals(bean1, "property[@name='serviceBRef']/@ref", "serviceB-typeB1Ref");
        assertXpathEquals(bean1, "property[@name='serviceB2Ref']/@ref", "serviceB2IdRef");
        assertXpathEquals(bean1, "property[@name='serviceB3Ref']/@ref", "serviceB-B3Ref");

        Node reference1 = getReferenceById("serviceB-typeB1Ref");
        assertXpathEquals(reference1, "@interface", ServiceB.class.getName());
        assertXpathEquals(reference1, "@filter", "(type=B1Ref)");

        Node reference2 = getReferenceById("serviceB2IdRef");
        assertXpathEquals(reference2, "@interface", ServiceB.class.getName());
        assertXpathEquals(reference2, "@filter", "(type=B2Ref)");

        Node reference3 = getReferenceById("serviceB-B3Ref");
        assertXpathEquals(reference3, "@interface", ServiceB.class.getName());
        assertXpathEquals(reference3, "@component-name", "B3Ref");
    }

    @Test
    public void testLazyWithTrueBeanHasActivationEager() throws Exception {
        Node bean = getBeanById("beanWithSetters");

        assertXpathEquals(bean, "@activation", "eager");
    }

    @Test
    public void testLazyBeanHasActivationLazy() throws Exception {
        Node bean = getBeanById("myBean1");

        assertXpathEquals(bean, "@activation", "lazy");
    }

    @Test
    public void testBeanWithoutLazyAnnotationHasNotActivationAttribute() throws Exception {
        Node bean1 = getBeanById("myBean3");

        assertXpathDoesNotExist(bean1, "@activation");
    }

    @Test
    public void testLazyProducedBeanOverriddenByFactoryMethodAnnotation() throws Exception {
        Node bean = getBeanById("producedEager");

        assertXpathEquals(bean, "@activation", "eager");
    }

    @Test
    public void testBeanWithoutDependsOnHasNotDependsOnAttribute() throws Exception {
        Node bean = getBeanById("beanWithSetters");

        assertXpathDoesNotExist(bean, "@depends-on");
    }

    @Test
    public void testBeanWithEmptyDependsOnHasNotDependsOnAttribute() throws Exception {
        Node bean = getBeanById("myBean6");

        assertXpathDoesNotExist(bean, "@depends-on");
    }

    @Test
    public void testBeanWithOneIdInDependsOnHasDependsOnAttribute() throws Exception {
        Node bean = getBeanById("myBean5");

        assertXpathEquals(bean, "@depends-on", "myBean6");
    }

    @Test
    public void testBeanWithTwoIdInDependsOnHasDependsOnAttribute() throws Exception {
        Node bean = getBeanById("myBean4");

        assertXpathEquals(bean, "@depends-on", "myBean5 myBean6");
    }

    @Test
    public void testProducedBeanMetohodWithoutDependsOnHasNotDependsOnAttribute() throws Exception {
        Node bean = getBeanById("produced1");

        assertXpathDoesNotExist(bean, "@depends-on");
    }

    @Test
    public void testProducedBeanMethodWithDependsOnHasDependsOnAttribute() throws Exception {
        Node bean = getBeanById("produced2");

        assertXpathEquals(bean, "@depends-on", "produced1");
    }

    private void assertXpathDoesNotExist(Node node, String xpathExpression) throws XPathExpressionException {
        assertXpathEquals(node, "count(" + xpathExpression + ")", "0");
    }

    private void assertXpathEquals(Node node, String xpathExpression, String expected) throws XPathExpressionException {
        assertEquals(expected, xpath.evaluate(xpathExpression, node));
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
