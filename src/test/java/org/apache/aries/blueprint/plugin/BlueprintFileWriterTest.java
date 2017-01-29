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
import org.apache.aries.blueprint.plugin.test.ServiceD;
import org.apache.aries.blueprint.plugin.test.referencelistener.ReferenceListenerToProduceWithoutAnnotation;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xbean.finder.ClassFinder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.aries.blueprint.plugin.FilteredClassFinder.findClasses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BlueprintFileWriterTest {

    private static final String NS_JPA = "http://aries.apache.org/xmlns/jpa/v1.1.0";
    private static final String NS_TX1 = "http://aries.apache.org/xmlns/transactions/v1.2.0";

    private static XPath xpath;
    private static Document document;
    private static byte[] xmlAsBytes;

    @BeforeClass
    public static void setUp() throws Exception {
        ClassFinder classFinder = new ClassFinder(BlueprintFileWriterTest.class.getClassLoader());
        Set<Class<?>> beanClasses = findClasses(classFinder, Arrays.asList(
                MyBean1.class.getPackage().getName(),
                ReferenceListenerToProduceWithoutAnnotation.class.getPackage().getName()
        ));
        Set<String> namespaces = new HashSet<String>(Arrays.asList(NS_JPA, NS_TX1));
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put("ex.t", "1");
        customParameters.put("example.p1", "v1");
        customParameters.put("example.p2", "v2");
        BlueprintConfigurationImpl blueprintConfiguration = new BlueprintConfigurationImpl(namespaces, null, customParameters);
        Context context = new Context(blueprintConfiguration, beanClasses);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new BlueprintFileWriter(os).generate(context);
        System.out.println(os.toString("UTF-8"));

        xmlAsBytes = os.toByteArray();
        document = readToDocument(xmlAsBytes, false);
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
    public void testGenerateServiceWithRanking() throws Exception {
        Node serviceWithRanking = getServiceByRef("serviceWithRanking");

        assertXpathDoesNotExist(serviceWithRanking, "@interface");
        assertXpathEquals(serviceWithRanking, "@auto-export", "interfaces");
        assertXpathDoesNotExist(serviceWithRanking, "interfaces");
        assertXpathEquals(serviceWithRanking, "@ranking", "100");
        assertXpathEquals(serviceWithRanking, "count(service-properties/entry)", "0");
        assertXpathDoesNotExist(serviceWithRanking, "service-properties/entry[@key='service.ranking']");
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
        assertXpathEquals(service, "@ranking", "100");
        assertXpathEquals(service, "count(service-properties/entry)", "2");
        assertXpathEquals(service, "service-properties/entry[@key='n1']/@value", "v1");
        assertXpathEquals(service, "service-properties/entry[@key='n2']/@value", "v2");
        assertXpathDoesNotExist(service, "service-properties/entry[@key='service.ranking']");
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
    public void testGenerateReferenceWithoutFilterAndComponentName() throws Exception {
        Node reference = getReferenceById("serviceD");
        assertXpathEquals(reference, "@interface", ServiceD.class.getName());
        assertXpathDoesNotExist(reference, "@filter");
        assertXpathDoesNotExist(reference, "@component-name");
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

    @Test
    public void testInitContextHandler() throws Exception {
        Node example1 = (Node) xpath.evaluate("/blueprint/example[@id='p1']", document, XPathConstants.NODE);
        Node example2 = (Node) xpath.evaluate("/blueprint/example[@id='p2']", document, XPathConstants.NODE);

        assertXpathEquals(example1, "@value", "v1");
        assertXpathEquals(example2, "@value", "v2");
    }

    @Test
    public void testProducesWithConfigProperty() throws Exception {
        Node bean = getBeanById("producedWithConfigProperty");
        assertXpathEquals(bean, "@class", "org.apache.aries.blueprint.plugin.test.MyProducedWithConstructor");
        assertXpathEquals(bean, "@factory-ref", "beanWithConfig");
        assertXpathEquals(bean, "@factory-method", "createBean");
        assertXpathEquals(bean, "@scope", "prototype");
        assertXpathEquals(bean, "argument/@value", "1000");
    }

    @Test
    public void testConfigPropertiesInjection() throws Exception {
        Node bean = getBeanById("beanWithConfigurationProperties");
        assertXpathEquals(bean, "@class", "org.apache.aries.blueprint.plugin.test.BeanWithConfigurationProperties");
        assertXpathEquals(bean, "argument[1]/@ref", "testProps5");
        assertXpathEquals(bean, "argument[2]/@ref", "properties-aries-test6-false");
        assertXpathEquals(bean, "property[@name='prop1']/@ref", "properties-aries-test1-true");
        assertXpathEquals(bean, "property[@name='prop2']/@ref", "testProps2");
        assertXpathEquals(bean, "property[@name='prop3']/@ref", "properties-aries-test3-true");
        assertXpathEquals(bean, "property[@name='prop4']/@ref", "testProps4");
        assertXpathEquals(bean, "property[@name='prop7']/@ref", "properties-aries-test7-false");
    }

    @Test
    public void testGenerateCmConfigProperties() throws Exception {
        Node testProps5 = getCmPropertiesById("testProps5");
        assertXpathEquals(testProps5, "@persistent-id", "aries.test5");
        assertXpathEquals(testProps5, "@update", "true");

        Node testProps6 = getCmPropertiesById("properties-aries-test6-false");
        assertXpathEquals(testProps6, "@persistent-id", "aries.test6");
        assertXpathEquals(testProps6, "@update", "false");

        Node testProps1 = getCmPropertiesById("properties-aries-test1-true");
        assertXpathEquals(testProps1, "@persistent-id", "aries.test1");
        assertXpathEquals(testProps1, "@update", "true");

        Node testProps2 = getCmPropertiesById("testProps2");
        assertXpathEquals(testProps2, "@persistent-id", "aries.test2");
        assertXpathEquals(testProps2, "@update", "false");

        Node testProps3 = getCmPropertiesById("properties-aries-test3-true");
        assertXpathEquals(testProps3, "@persistent-id", "aries.test3");
        assertXpathEquals(testProps3, "@update", "true");

        Node testProps4 = getCmPropertiesById("testProps4");
        assertXpathEquals(testProps4, "@persistent-id", "aries.test4");
        assertXpathEquals(testProps4, "@update", "false");

        Node testProps7 = getCmPropertiesById("properties-aries-test7-false");
        assertXpathEquals(testProps7, "@persistent-id", "aries.test7");
        assertXpathEquals(testProps7, "@update", "false");
    }

    @Test
    public void testProducesWithConfigProperties() throws Exception {
        Node withProperties8 = getBeanById("withProperties8");
        assertXpathEquals(withProperties8, "@class", "org.apache.aries.blueprint.plugin.test.MyProducedWithConstructor");
        assertXpathEquals(withProperties8, "argument/@ref", "properties-aries-test8-false");

        Node testProps8 = getCmPropertiesById("properties-aries-test8-false");
        assertXpathEquals(testProps8, "@persistent-id", "aries.test8");
        assertXpathEquals(testProps8, "@update", "false");

        Node withProperties9 = getBeanById("withProperties9");
        assertXpathEquals(withProperties9, "@class", "org.apache.aries.blueprint.plugin.test.MyProducedWithConstructor");
        assertXpathEquals(withProperties9, "argument/@ref", "testProps9");

        Node testProps9 = getCmPropertiesById("testProps9");
        assertXpathEquals(testProps9, "@persistent-id", "aries.test9");
        assertXpathEquals(testProps9, "@update", "true");
    }

    @Test
    public void referenceListnerForReferenceList() throws Exception {
        assertNotNull(getBeanById("referenceListenerListBean"));

        Node referenceList = getReferenceListById("serviceAList-a-bc");
        assertXpathEquals(referenceList, "@filter", "(b=c)");
        assertXpathEquals(referenceList, "@component-name", "a");
        assertXpathEquals(referenceList, "@availability", "mandatory");
        assertXpathEquals(referenceList, "@interface", ServiceA.class.getName());
        assertXpathEquals(referenceList, "reference-listener/@ref", "referenceListenerListBean");
        assertXpathEquals(referenceList, "reference-listener/@bind-method", "add");
        assertXpathEquals(referenceList, "reference-listener/@unbind-method", "remove");
    }

    @Test
    public void referenceListnerForReference() throws Exception {
        assertNotNull(getBeanById("referenceListenerBeanWithNameWithoutMethods"));

        Node reference = getReferenceById("serviceAReferenceWithoutMethods");
        assertXpathDoesNotExist(reference, "@filter");
        assertXpathDoesNotExist(reference, "@component-name");
        assertXpathEquals(reference, "@availability", "optional");
        assertXpathEquals(reference, "@interface", ServiceA.class.getName());
        assertXpathEquals(reference, "reference-listener/@ref", "referenceListenerBeanWithNameWithoutMethods");
        assertXpathDoesNotExist(reference, "reference-listener/@bind-method");
        assertXpathDoesNotExist(reference, "reference-listener/@unbind-method");
    }

    @Test
    public void referenceListnerForReferenceWithouMethodAnnotations() throws Exception {
        assertNotNull(getBeanById("referenceListenerBeanWithoutMethodsAnnotation"));

        Node reference = getReferenceListById("serviceAReference");
        assertXpathDoesNotExist(reference, "@filter");
        assertXpathDoesNotExist(reference, "@component-name");
        assertXpathEquals(reference, "@availability", "optional");
        assertXpathEquals(reference, "@interface", ServiceA.class.getName());
        assertXpathEquals(reference, "reference-listener/@ref", "referenceListenerBeanWithoutMethodsAnnotation");
        assertXpathEquals(reference, "reference-listener/@bind-method", "addMe");
        assertXpathEquals(reference, "reference-listener/@unbind-method", "removeMe");
    }

    @Test
    public void produceReferenceListnerForReference() throws Exception {
        assertNotNull(getBeanById("referenceListenerProducer"));

        Node referenceListenerToProduceForSingle = getBeanById("referenceListenerToProduceForSingle");
        assertXpathEquals(referenceListenerToProduceForSingle, "@factory-ref", "referenceListenerProducer");
        assertXpathEquals(referenceListenerToProduceForSingle, "@factory-method", "single");

        Node reference = getReferenceById("serviceB-producer123-b123");
        assertXpathEquals(reference, "@filter", "(b=123)");
        assertXpathEquals(reference, "@component-name", "producer123");
        assertXpathEquals(reference, "@availability", "optional");
        assertXpathEquals(reference, "@interface", ServiceB.class.getName());
        assertXpathEquals(reference, "reference-listener/@ref", "referenceListenerToProduceForSingle");
        assertXpathEquals(reference, "reference-listener/@bind-method", "register");
        assertXpathEquals(reference, "reference-listener/@unbind-method", "unregister");
    }

    @Test
    public void produceReferenceListnerForReferenceList() throws Exception {
        assertNotNull(getBeanById("referenceListenerProducer"));

        Node referenceListenerToProduceForList = getBeanById("referenceListenerToProduceForList");
        assertXpathEquals(referenceListenerToProduceForList, "@factory-ref", "referenceListenerProducer");
        assertXpathEquals(referenceListenerToProduceForList, "@factory-method", "list");

        Node referenceList = getReferenceListById("referenceListForProducer");
        assertXpathEquals(referenceList, "@filter", "(b=456)");
        assertXpathEquals(referenceList, "@component-name", "producer456");
        assertXpathEquals(referenceList, "@availability", "optional");
        assertXpathEquals(referenceList, "@interface", ServiceB.class.getName());
        assertXpathEquals(referenceList, "reference-listener/@ref", "referenceListenerToProduceForList");
        assertXpathEquals(referenceList, "reference-listener/@bind-method", "addMe");
        assertXpathEquals(referenceList, "reference-listener/@unbind-method", "removeMe");
    }

    @Test
    public void produceReferenceListnerForReferenceListWithOverrideAnnotatedMethods() throws Exception {
        assertNotNull(getBeanById("referenceListenerProducer"));

        Node referenceListenerToProduceWithBindingMethodsByName = getBeanById("referenceListenerToProduceWithBindingMethodsByName");
        assertXpathEquals(referenceListenerToProduceWithBindingMethodsByName, "@factory-ref", "referenceListenerProducer");
        assertXpathEquals(referenceListenerToProduceWithBindingMethodsByName, "@factory-method", "listWithDefinedMethods");

        Node referenceList = getReferenceListById("serviceBList");
        assertXpathDoesNotExist(referenceList, "@filter");
        assertXpathDoesNotExist(referenceList, "@component-name");
        assertXpathEquals(referenceList, "@availability", "mandatory");
        assertXpathEquals(referenceList, "@interface", ServiceB.class.getName());
        assertXpathEquals(referenceList, "reference-listener/@ref", "referenceListenerToProduceWithBindingMethodsByName");
        assertXpathEquals(referenceList, "reference-listener/@bind-method", "addMe");
        assertXpathEquals(referenceList, "reference-listener/@unbind-method", "removeMe");
    }

    @Test
    public void generatedXmlIsValid() throws Exception {
        Document document = readToDocument(xmlAsBytes, true);

        Source[] schemas = new StreamSource[]{
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/example.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/blueprint.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/ext/impl/blueprint-ext.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/ext/impl/blueprint-ext-1.1.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/ext/impl/blueprint-ext-1.2.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/ext/impl/blueprint-ext-1.3.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/ext/impl/blueprint-ext-1.4.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/ext/impl/blueprint-ext-1.5.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/transaction/parsing/transactionv12.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/jpa/blueprint/namespace/jpa_110.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/compendium/cm/blueprint-cm-1.0.0.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/compendium/cm/blueprint-cm-1.1.0.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/compendium/cm/blueprint-cm-1.3.0.xsd")),
                new StreamSource(BlueprintFileWriterTest.class.getResourceAsStream("/schema/org/apache/aries/blueprint/compendium/cm/blueprint-cm-1.2.0.xsd"))
        };

        Source xmlFile = new DOMSource(document);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemas);
        Validator validator = schema.newValidator();
        validator.validate(xmlFile);
    }

    private void assertXpathDoesNotExist(Node node, String xpathExpression) throws XPathExpressionException {
        assertXpathEquals(node, "count(" + xpathExpression + ")", "0");
    }

    private void assertXpathEquals(Node node, String xpathExpression, String expected) throws XPathExpressionException {
        assertEquals(expected, xpath.evaluate(xpathExpression, node));
    }

    private static Document readToDocument(byte[] xmlAsBytes, boolean nameSpaceAware) throws ParserConfigurationException,
            SAXException, IOException {

        InputStream is = new ByteArrayInputStream(xmlAsBytes);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(nameSpaceAware);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        return builder.parse(is);
    }

    private static Node getBeanById(String id) throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/bean[@id='" + id + "']", document, XPathConstants.NODE);
    }

    private static Node getCmPropertiesById(String id) throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/cm-properties[@id='" + id + "']", document, XPathConstants.NODE);
    }

    private static Node getServiceByRef(String id) throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/service[@ref='" + id + "']", document, XPathConstants.NODE);
    }

    private static Node getReferenceById(String id) throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/reference[@id='" + id + "']", document, XPathConstants.NODE);
    }

    private static Node getReferenceListById(String id) throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/reference-list[@id='" + id + "']", document, XPathConstants.NODE);
    }

}
