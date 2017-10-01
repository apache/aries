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
import org.apache.aries.blueprint.plugin.model.Blueprint;
import org.apache.aries.blueprint.plugin.model.TransactionalDef;
import org.apache.aries.blueprint.plugin.test.MyBean1;
import org.apache.aries.blueprint.plugin.test.MyProduced;
import org.apache.aries.blueprint.plugin.test.interfaces.ServiceA;
import org.apache.aries.blueprint.plugin.test.interfaces.ServiceB;
import org.apache.aries.blueprint.plugin.test.interfaces.ServiceD;
import org.apache.aries.blueprint.plugin.test.bean.BasicBean;
import org.apache.aries.blueprint.plugin.test.bean.BeanWithCallbackMethods;
import org.apache.aries.blueprint.plugin.test.bean.NamedBean;
import org.apache.aries.blueprint.plugin.test.bean.SimpleProducedBean;
import org.apache.aries.blueprint.plugin.test.reference.BeanWithReferences;
import org.apache.aries.blueprint.plugin.test.reference.Ref1;
import org.apache.aries.blueprint.plugin.test.reference.Ref2;
import org.apache.aries.blueprint.plugin.test.reference.Ref3;
import org.apache.aries.blueprint.plugin.test.reference.Ref4;
import org.apache.aries.blueprint.plugin.test.referencelistener.ReferenceListenerToProduceWithoutAnnotation;
import org.apache.aries.blueprint.plugin.test.service.Service1;
import org.apache.aries.blueprint.plugin.test.service.Service2;
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
        long start = System.currentTimeMillis();
        Set<Class<?>> beanClasses = findClasses(classFinder, Arrays.asList(
                MyBean1.class.getPackage().getName(),
                ReferenceListenerToProduceWithoutAnnotation.class.getPackage().getName(),
                BeanWithReferences.class.getPackage().getName()
        ));
        Set<String> namespaces = new HashSet<>(Arrays.asList(NS_JPA, NS_TX1));
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put("ex.t", "1");
        customParameters.put("example.p1", "v1");
        customParameters.put("example.p2", "v2");
        BlueprintConfigurationImpl blueprintConfiguration = new BlueprintConfigurationImpl(namespaces, null, customParameters, null, null);
        Blueprint blueprint = new Blueprint(blueprintConfiguration, beanClasses);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new BlueprintFileWriter(os).write(blueprint);

        xmlAsBytes = os.toByteArray();
        System.out.println("Generation took " + (System.currentTimeMillis() - start) + " millis");
        System.out.println(new String(xmlAsBytes, "UTF-8"));

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
        assertXpathEquals(bean, "@class", "org.apache.aries.blueprint.plugin.test.configuration.BeanWithConfigurationProperties");
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

    @Test
    public void beanAnnotationCreatesBasicBean() throws Exception {
        Node bean = getBeanById("basicBean");
        assertXpathEquals(bean, "@class", BasicBean.class.getName());
        assertXpathDoesNotExist(bean, "@scope");
        assertXpathDoesNotExist(bean, "@activation");
        assertXpathDoesNotExist(bean, "@depends-on");
        assertXpathDoesNotExist(bean, "@init-method");
        assertXpathDoesNotExist(bean, "@destroy-method");
        assertXpathDoesNotExist(bean, "@factory-ref");
        assertXpathDoesNotExist(bean, "@factory-method");
    }

    @Test
    public void beanAnnotationCreatesNamedBean() throws Exception {
        Node bean = getBeanById("namedBean1");
        assertXpathEquals(bean, "@class", NamedBean.class.getName());
        assertXpathEquals(bean, "@activation", "eager");
        assertXpathEquals(bean, "@scope", "prototype");
        assertXpathDoesNotExist(bean, "@depends-on");
        assertXpathDoesNotExist(bean, "@init-method");
        assertXpathDoesNotExist(bean, "@destroy-method");
        assertXpathDoesNotExist(bean, "@factory-ref");
        assertXpathDoesNotExist(bean, "@factory-method");
    }

    @Test
    public void beanAnnotationCreatesBeanWithCallbackMethods() throws Exception {
        Node bean = getBeanById("beanWithCallbackMethods");
        assertXpathEquals(bean, "@class", BeanWithCallbackMethods.class.getName());
        assertXpathEquals(bean, "@scope", "prototype");
        assertXpathEquals(bean, "@activation", "lazy");
        assertXpathEquals(bean, "@depends-on", "basicBean namedBean1");
        assertXpathEquals(bean, "@init-method", "init");
        assertXpathEquals(bean, "@destroy-method", "destroy");
        assertXpathDoesNotExist(bean, "@factory-ref");
        assertXpathDoesNotExist(bean, "@factory-method");
    }

    @Test
    public void beanAnnotationProducesSimpleBean() throws Exception {
        Node bean = getBeanById("simpleProducedBean1");
        assertXpathEquals(bean, "@class", SimpleProducedBean.class.getName());
        assertXpathDoesNotExist(bean, "@scope");
        assertXpathDoesNotExist(bean, "@activation");
        assertXpathDoesNotExist(bean, "@depends-on");
        assertXpathDoesNotExist(bean, "@init-method");
        assertXpathDoesNotExist(bean, "@destroy-method");
        assertXpathEquals(bean, "@factory-ref", "basicBean");
        assertXpathEquals(bean, "@factory-method", "getBean1");
    }

    @Test
    public void beanAnnotationProducesPrototypeBean() throws Exception {
        Node bean = getBeanById("simpleProducedBean2");
        assertXpathEquals(bean, "@class", SimpleProducedBean.class.getName());
        assertXpathEquals(bean, "@activation", "eager");
        assertXpathEquals(bean, "@scope", "prototype");
        assertXpathDoesNotExist(bean, "@depends-on");
        assertXpathDoesNotExist(bean, "@init-method");
        assertXpathDoesNotExist(bean, "@destroy-method");
        assertXpathEquals(bean, "@factory-ref", "basicBean");
        assertXpathEquals(bean, "@factory-method", "getBean2");
    }

    @Test
    public void beanAnnotationProducesMethodWithCallbacks() throws Exception {
        Node bean = getBeanById("simpleProducedBean3");
        assertXpathEquals(bean, "@class", SimpleProducedBean.class.getName());
        assertXpathEquals(bean, "@scope", "prototype");
        assertXpathEquals(bean, "@activation", "lazy");
        assertXpathEquals(bean, "@depends-on", "simpleProducedBean1 simpleProducedBean2");
        assertXpathEquals(bean, "@init-method", "init1");
        assertXpathEquals(bean, "@destroy-method", "destroy1");
        assertXpathEquals(bean, "@factory-ref", "basicBean");
        assertXpathEquals(bean, "@factory-method", "getBean3");
    }

    @Test
    public void typedProperties() throws Exception {
        Node service = getServiceByRef("serviceWithTypedParameters");
        assertXpathEquals(service, "count(service-properties/entry)", "6");
        assertXpathEquals(service, "service-properties/entry[@key='test1']/@value", "test");

        assertXpathDoesNotExist(service, "service-properties/entry[@key='test2']/@value");
        assertXpathEquals(service, "service-properties/entry[@key='test2']/value", "15");
        assertXpathEquals(service, "service-properties/entry[@key='test2']/value/@type", "java.lang.Integer");

        assertXpathDoesNotExist(service, "service-properties/entry[@key='test3']/@value");
        assertXpathEquals(service, "service-properties/entry[@key='test3']/value", "true");
        assertXpathEquals(service, "service-properties/entry[@key='test3']/value/@type", "java.lang.Boolean");

        assertXpathDoesNotExist(service, "service-properties/entry[@key='test4']/@value");
        assertXpathEquals(service, "service-properties/entry[@key='test4']/array/value[1]", "val1");
        assertXpathEquals(service, "service-properties/entry[@key='test4']/array/value[2]", "val2");
        assertXpathDoesNotExist(service, "service-properties/entry[@key='test4']/array/@value-type");

        assertXpathDoesNotExist(service, "service-properties/entry[@key='test5']/@value");
        assertXpathEquals(service, "service-properties/entry[@key='test5']/array/value[1]", "1");
        assertXpathEquals(service, "service-properties/entry[@key='test5']/array/value[2]", "2");
        assertXpathEquals(service, "service-properties/entry[@key='test5']/array/value[3]", "3");
        assertXpathEquals(service, "service-properties/entry[@key='test5']/array/@value-type", "java.lang.Short");

        assertXpathDoesNotExist(service, "service-properties/entry[@key='test6']/@value");
        assertXpathEquals(service, "service-properties/entry[@key='test6']/array/value[1]", "1.5");
        assertXpathEquals(service, "service-properties/entry[@key='test6']/array/value[2]", "0.8");
        assertXpathEquals(service, "service-properties/entry[@key='test6']/array/value[3]", "-7.1");
        assertXpathEquals(service, "service-properties/entry[@key='test6']/array/@value-type", "java.lang.Double");

    }

    @Test
    public void shouldInjectDependencyByQualifierFromFactory() throws Exception {

        Node bean1 = getBeanById("testBean1");
        assertXpathEquals(bean1, "@factory-method", "create1");

        Node bean2 = getBeanById("testBean2");
        assertXpathEquals(bean2, "@factory-method", "create2");

        Node consumer = getBeanById("testConsumer");
        assertXpathEquals(consumer, "argument[1]/@ref", "testBean1");
        assertXpathEquals(consumer, "argument[2]/@ref", "testBean2");
    }

    @Test
    public void shouldGeneratePropertyPlaceholder() throws Exception {
        Node propertyPlaceholder = getPropertyPlaceholderByPersistentId("org.apache.aries.my");
        assertXpathEquals(propertyPlaceholder, "@placeholder-prefix", "$[");
        assertXpathEquals(propertyPlaceholder, "@placeholder-suffix", "]");
        assertXpathEquals(propertyPlaceholder, "@update-strategy", "reload");
        assertXpathEquals(propertyPlaceholder, "count(default-properties/property)", "2");
        assertXpathEquals(propertyPlaceholder, "default-properties/property[@name='title']/@value", "My Title");
        assertXpathEquals(propertyPlaceholder, "default-properties/property[@name='test2']/@value", "v2");
    }

    @Test
    public void shouldInjectListViaField() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(property[@name='listFieldInject']/list/ref)", "4");
        assertXpathEquals(bean, "property[@name='listFieldInject']/list/ref[1]/@component-id", "i1Impl1");
        assertXpathEquals(bean, "property[@name='listFieldInject']/list/ref[2]/@component-id", "i1Impl2");
        assertXpathEquals(bean, "property[@name='listFieldInject']/list/ref[3]/@component-id", "i1Impl3Annotated");
        assertXpathEquals(bean, "property[@name='listFieldInject']/list/ref[4]/@component-id", "i1Impl4Annotated");
    }

    @Test
    public void shouldInjectSetViaField() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(property[@name='setFieldInject']/set/ref)", "4");
        assertXpathEquals(bean, "property[@name='setFieldInject']/set/ref[1]/@component-id", "i1Impl1");
        assertXpathEquals(bean, "property[@name='setFieldInject']/set/ref[2]/@component-id", "i1Impl2");
        assertXpathEquals(bean, "property[@name='setFieldInject']/set/ref[3]/@component-id", "i1Impl3Annotated");
        assertXpathEquals(bean, "property[@name='setFieldInject']/set/ref[4]/@component-id", "i1Impl4Annotated");
    }

    @Test
    public void shouldInjectArrayViaField() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(property[@name='arrayFieldInject']/array/ref)", "3");
        assertXpathEquals(bean, "property[@name='arrayFieldInject']/array/ref[1]/@component-id", "i2Impl1");
        assertXpathEquals(bean, "property[@name='arrayFieldInject']/array/ref[2]/@component-id", "i2Impl2Annotated");
        assertXpathEquals(bean, "property[@name='arrayFieldInject']/array/ref[3]/@component-id", "i2Impl3Annotated");
    }

    @Test
    public void shouldInjectAnnotatedSetViaField() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(property[@name='annotatedSetFieldInject']/set/ref)", "2");
        assertXpathEquals(bean, "property[@name='annotatedSetFieldInject']/set/ref[1]/@component-id", "i1Impl3Annotated");
        assertXpathEquals(bean, "property[@name='annotatedSetFieldInject']/set/ref[2]/@component-id", "i1Impl4Annotated");
    }

    @Test
    public void shouldInjectListViaSetter() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(property[@name='listSetterInject']/list/ref)", "4");
        assertXpathEquals(bean, "property[@name='listSetterInject']/list/ref[1]/@component-id", "i1Impl1");
        assertXpathEquals(bean, "property[@name='listSetterInject']/list/ref[2]/@component-id", "i1Impl2");
        assertXpathEquals(bean, "property[@name='listSetterInject']/list/ref[3]/@component-id", "i1Impl3Annotated");
        assertXpathEquals(bean, "property[@name='listSetterInject']/list/ref[4]/@component-id", "i1Impl4Annotated");
    }

    @Test
    public void shouldInjectSetViaSetter() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(property[@name='setSetterInject']/set/ref)", "3");
        assertXpathEquals(bean, "property[@name='setSetterInject']/set/ref[1]/@component-id", "i2Impl1");
        assertXpathEquals(bean, "property[@name='setSetterInject']/set/ref[2]/@component-id", "i2Impl2Annotated");
        assertXpathEquals(bean, "property[@name='setSetterInject']/set/ref[3]/@component-id", "i2Impl3Annotated");
    }

    @Test
    public void shouldInjectArrayViaSetter() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(property[@name='arraySetterInject']/array/ref)", "4");
        assertXpathEquals(bean, "property[@name='arraySetterInject']/array/ref[1]/@component-id", "i1Impl1");
        assertXpathEquals(bean, "property[@name='arraySetterInject']/array/ref[2]/@component-id", "i1Impl2");
        assertXpathEquals(bean, "property[@name='arraySetterInject']/array/ref[3]/@component-id", "i1Impl3Annotated");
        assertXpathEquals(bean, "property[@name='arraySetterInject']/array/ref[4]/@component-id", "i1Impl4Annotated");
    }

    @Test
    public void shouldInjectAnnotatedArrayViaSetter() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(property[@name='annotatedArraySetterInject']/array/ref)", "2");
        assertXpathEquals(bean, "property[@name='annotatedArraySetterInject']/array/ref[1]/@component-id", "i2Impl2Annotated");
        assertXpathEquals(bean, "property[@name='annotatedArraySetterInject']/array/ref[2]/@component-id", "i2Impl3Annotated");
    }

    @Test
    public void shouldInjectListViaConstructor() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(argument[1]/list/ref)", "4");
        assertXpathEquals(bean, "argument[1]/list/ref[1]/@component-id", "i1Impl1");
        assertXpathEquals(bean, "argument[1]/list/ref[2]/@component-id", "i1Impl2");
        assertXpathEquals(bean, "argument[1]/list/ref[3]/@component-id", "i1Impl3Annotated");
        assertXpathEquals(bean, "argument[1]/list/ref[4]/@component-id", "i1Impl4Annotated");
    }

    @Test
    public void shouldInjectSetViaConstructor() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(argument[2]/set/ref)", "4");
        assertXpathEquals(bean, "argument[2]/set/ref[1]/@component-id", "i1Impl1");
        assertXpathEquals(bean, "argument[2]/set/ref[2]/@component-id", "i1Impl2");
        assertXpathEquals(bean, "argument[2]/set/ref[3]/@component-id", "i1Impl3Annotated");
        assertXpathEquals(bean, "argument[2]/set/ref[4]/@component-id", "i1Impl4Annotated");
    }

    @Test
    public void shouldInjectArrayViaConstructor() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(argument[3]/array/ref)", "3");
        assertXpathEquals(bean, "argument[3]/array/ref[1]/@component-id", "i2Impl1");
        assertXpathEquals(bean, "argument[3]/array/ref[2]/@component-id", "i2Impl2Annotated");
        assertXpathEquals(bean, "argument[3]/array/ref[3]/@component-id", "i2Impl3Annotated");
    }

    @Test
    public void shouldInjectAnnotatedListViaConstructor() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(argument[4]/list/ref)", "2");
        assertXpathEquals(bean, "argument[4]/list/ref[1]/@component-id", "i1Impl3Annotated");
        assertXpathEquals(bean, "argument[4]/list/ref[2]/@component-id", "i1Impl4Annotated");
    }

    @Test
    public void shouldInjectEmptyListViaConstructor() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(argument[5]/list)", "1");
        assertXpathEquals(bean, "count(argument[5]/list/ref)", "0");
    }

    @Test
    public void shouldInjectEmptySetViaConstructor() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(argument[6]/set)", "1");
        assertXpathEquals(bean, "count(argument[6]/set/ref)", "0");
    }

    @Test
    public void shouldInjectEmptyArrayViaConstructor() throws Exception {
        Node bean = getBeanById("beanWithCollections");
        assertXpathEquals(bean, "count(argument[7]/array)", "1");
        assertXpathEquals(bean, "count(argument[7]/array/ref)", "0");
    }

    @Test
    public void shouldFindTypeConverters() throws Exception {
        Node typeConverters = getTypeConverters();
        assertXpathEquals(typeConverters, "count(*)", "2");
        assertXpathEquals(typeConverters, "ref[1]/@component-id", "converter1");
        assertXpathEquals(typeConverters, "ref[2]/@component-id", "converter2");
    }

    @Test
    public void shouldInjectReferenceViaField() throws Exception {
        Node bean = getBeanById("beanWithReferences");
        assertXpathEquals(bean, "property[@name='ref1Field']/@ref", "ref1");
        assertXpathEquals(bean, "property[@name='myRef1Field']/@ref", "myRef1");
        assertXpathEquals(bean, "property[@name='myRef1FieldAllProps']/@ref", "ref1-a453-r1-optional-2000");
        assertXpathEquals(bean, "property[@name='myRef1FieldFilter']/@ref", "ref1-x1");
    }

    @Test
    public void shouldGenerateReferenceFromBeanField() throws Exception {
        Node ref1 = getReferenceById("ref1");
        assertXpathEquals(ref1, "@interface", Ref1.class.getName());
        Node myRef1 = getReferenceById("myRef1");
        assertXpathEquals(myRef1, "@interface", Ref1.class.getName());
        Node ref1a453r1 = getReferenceById("ref1-a453-r1-optional-2000");
        assertXpathEquals(ref1a453r1, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1a453r1, "@component-name", "r1");
        assertXpathEquals(ref1a453r1, "@filter", "(a=453)");
        assertXpathEquals(ref1a453r1, "@timeout", "2000");
        assertXpathEquals(ref1a453r1, "@availability", "optional");
        Node ref1x1 = getReferenceById("ref1-x1");
        assertXpathEquals(ref1x1, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1x1, "@filter", "(x=1)");
        assertXpathEquals(ref1x1, "count(@component-name)", "0");
    }

    @Test
    public void shouldInjectReferenceViaSetter() throws Exception {
        Node bean = getBeanById("beanWithReferences");
        assertXpathEquals(bean, "property[@name='ref2Setter']/@ref", "ref2");
        assertXpathEquals(bean, "property[@name='ref2SetterNamed']/@ref", "myRef2");
        assertXpathEquals(bean, "property[@name='ref2SetterFull']/@ref", "ref2-b453-r2-optional-1000");
        assertXpathEquals(bean, "property[@name='ref2SetterComponent']/@ref", "ref2--blablabla");
    }

    @Test
    public void shouldGenerateReferenceFromBeanSetter() throws Exception {
        Node ref2 = getReferenceById("ref2");
        assertXpathEquals(ref2, "@interface", Ref2.class.getName());
        Node myRef2 = getReferenceById("myRef2");
        assertXpathEquals(myRef2, "@interface", Ref2.class.getName());
        Node ref1b453r2 = getReferenceById("ref2-b453-r2-optional-1000");
        assertXpathEquals(ref1b453r2, "@interface", Ref2.class.getName());
        assertXpathEquals(ref1b453r2, "@component-name", "r2");
        assertXpathEquals(ref1b453r2, "@filter", "(b=453)");
        assertXpathEquals(ref1b453r2, "@timeout", "1000");
        assertXpathEquals(ref1b453r2, "@availability", "optional");
        Node ref2blablabla = getReferenceById("ref2--blablabla");
        assertXpathEquals(ref2blablabla, "@interface", Ref2.class.getName());
        assertXpathEquals(ref2blablabla, "@component-name", "blablabla");
        assertXpathEquals(ref2blablabla, "count(@filter)", "0");
    }

    @Test
    public void shouldInjectReferenceViaConstructor() throws Exception {
        Node bean = getBeanById("beanWithReferences");
        assertXpathEquals(bean, "argument[1]/@ref", "ref1");
        assertXpathEquals(bean, "argument[2]/@ref", "ref2---optional-20000");
        assertXpathEquals(bean, "argument[3]/@ref", "ref1-y3");
        assertXpathEquals(bean, "argument[4]/@ref", "ref1--compForConstr");
        assertXpathEquals(bean, "argument[5]/@ref", "ref1-y3-compForConstr");
        assertXpathEquals(bean, "argument[6]/@ref", "ref1ForCons");
    }

    @Test
    public void shouldGenerateReferenceFromBeanConstructor() throws Exception {
        Node ref1 = getReferenceById("ref1");
        assertXpathEquals(ref1, "@interface", Ref1.class.getName());
        Node ref2optional20000 = getReferenceById("ref2---optional-20000");
        assertXpathEquals(ref2optional20000, "@interface", Ref2.class.getName());
        assertXpathEquals(ref2optional20000, "@timeout", "20000");
        assertXpathEquals(ref2optional20000, "@availability", "optional");
        Node ref1y3 = getReferenceById("ref1-y3");
        assertXpathEquals(ref1y3, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1y3, "count(@component-name)", "0");
        assertXpathEquals(ref1y3, "@filter", "(y=3)");
        Node ref1compForConstr = getReferenceById("ref1--compForConstr");
        assertXpathEquals(ref1compForConstr, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1compForConstr, "@component-name", "compForConstr");
        assertXpathEquals(ref1compForConstr, "count(@filter)", "0");
        Node ref1y3compForConstr = getReferenceById("ref1-y3-compForConstr");
        assertXpathEquals(ref1y3compForConstr, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1y3compForConstr, "@component-name", "compForConstr");
        assertXpathEquals(ref1y3compForConstr, "@filter", "(y=3)");
        Node ref1ForCons = getReferenceById("ref1ForCons");
        assertXpathEquals(ref1ForCons, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1ForCons, "@availability", "optional");
    }

    @Test
    public void shouldInjectReferenceToProducedBean() throws Exception {
        Node bean = getBeanById("producedWithReferences");
        assertXpathEquals(bean, "argument[1]/@ref", "ref3");
        assertXpathEquals(bean, "argument[2]/@ref", "ref4----20000");
        assertXpathEquals(bean, "argument[3]/@ref", "ref4---optional");
        assertXpathEquals(bean, "argument[4]/@ref", "ref3-y3");
        assertXpathEquals(bean, "argument[5]/@ref", "ref3--compForProduces");
        assertXpathEquals(bean, "argument[6]/@ref", "ref3-y3-compForProduces");
        assertXpathEquals(bean, "argument[7]/@ref", "ref3ForProduces");
    }

    @Test
    public void shouldGenerateReferenceFromProducedBean() throws Exception {
        Node ref3 = getReferenceById("ref3");
        assertXpathEquals(ref3, "@interface", Ref3.class.getName());
        Node ref420000 = getReferenceById("ref4----20000");
        assertXpathEquals(ref420000, "@interface", Ref4.class.getName());
        assertXpathEquals(ref420000, "@timeout", "20000");
        Node ref4optional = getReferenceById("ref4---optional");
        assertXpathEquals(ref4optional, "@interface", Ref4.class.getName());
        assertXpathEquals(ref4optional, "@availability", "optional");
        Node ref3y3 = getReferenceById("ref3-y3");
        assertXpathEquals(ref3y3, "@interface", Ref3.class.getName());
        assertXpathEquals(ref3y3, "count(@component-name)", "0");
        assertXpathEquals(ref3y3, "@filter", "(y=3)");
        Node ref3compForProduces = getReferenceById("ref3--compForProduces");
        assertXpathEquals(ref3compForProduces, "@interface", Ref3.class.getName());
        assertXpathEquals(ref3compForProduces, "@component-name", "compForProduces");
        assertXpathEquals(ref3compForProduces, "count(@filter)", "0");
        Node ref3y3compForProduces = getReferenceById("ref3-y3-compForProduces");
        assertXpathEquals(ref3y3compForProduces, "@interface", Ref3.class.getName());
        assertXpathEquals(ref3y3compForProduces, "@component-name", "compForProduces");
        assertXpathEquals(ref3y3compForProduces, "@filter", "(y=3)");
        Node ref1ForCons = getReferenceById("ref3ForProduces");
        assertXpathEquals(ref1ForCons, "@interface", Ref3.class.getName());
        assertXpathEquals(ref1ForCons, "@timeout", "1000");
    }

    @Test
    public void shouldGenerateSimplestServiceFromBean() throws XPathExpressionException {
        Node service = getServiceByRef("simplestService");
        assertXpathEquals(service, "@auto-export", "interfaces");
        assertXpathDoesNotExist(service, "service-properties");
    }

    @Test
    public void shouldGenerateServiceWithAllClassesFromBean() throws XPathExpressionException {
        Node service = getServiceByRef("serviceWithAllClasses");
        assertXpathEquals(service, "@auto-export", "all-classes");
    }

    @Test
    public void shouldGenerateServiceWithClassHierarchyFromBean() throws XPathExpressionException {
        Node service = getServiceByRef("serviceWithClassHierarchy");
        assertXpathEquals(service, "@auto-export", "class-hierarchy");
    }

    @Test
    public void shouldGenerateServiceWithOneInterfaceFromBean() throws XPathExpressionException {
        Node service = getServiceByRef("serviceWithOneInterface");
        assertXpathEquals(service, "count(@auto-export)", "0");
        assertXpathEquals(service, "count(interfaces)", "0");
        assertXpathEquals(service, "@interface", Service1.class.getName());
    }

    @Test
    public void shouldGenerateServiceWithManyInterfacesFromBean() throws XPathExpressionException {
        Node service = getServiceByRef("serviceWithManyInterfaces");
        assertXpathEquals(service, "count(@auto-export)", "0");
        assertXpathEquals(service, "count(@interface)", "0");
        assertXpathEquals(service, "count(interfaces/value)", "2");
        assertXpathEquals(service, "interfaces/value[1]", Service1.class.getName());
        assertXpathEquals(service, "interfaces/value[2]", Service2.class.getName());
    }

    @Test
    public void shouldGenerateServiceWithRankingFromBean() throws XPathExpressionException {
        Node service = getServiceByRef("serviceWithRankingParameter");
        assertXpathEquals(service, "@ranking", "1000");
    }

    @Test
    public void shouldGenerateServiceWithRankingAndPropertyFromBean() throws XPathExpressionException {
        Node service = getServiceByRef("serviceWithRankingAndProperty");
        assertXpathEquals(service, "@ranking", "2");
        assertXpathDoesNotExist(service, "service-properties");
    }

    @Test
    public void shouldGenerateServiceWithPropertiesFromBean() throws XPathExpressionException {
        Node service = getServiceByRef("serviceWithProperties");
        assertXpathEquals(service, "count(service-properties/entry)", "4");
        assertXpathEquals(service, "service-properties/entry[@key='oneValue']/@value", "test");
        assertXpathEquals(service, "count(service-properties/entry[@key='oneValue']/value)", "0");

        assertXpathEquals(service, "count(service-properties/entry[@key='intValue']/@value)", "0");
        assertXpathEquals(service, "service-properties/entry[@key='intValue']/value/@type", Integer.class.getName());
        assertXpathEquals(service, "service-properties/entry[@key='intValue']/value/text()", "1");

        assertXpathEquals(service, "count(service-properties/entry[@key='longArray']/@value)", "0");
        assertXpathEquals(service, "count(service-properties/entry[@key='longArray']/value)", "0");
        assertXpathEquals(service, "count(service-properties/entry[@key='longArray']/array/value)", "3");
        assertXpathEquals(service, "service-properties/entry[@key='longArray']/array/@value-type", Long.class.getName());
        assertXpathEquals(service, "service-properties/entry[@key='longArray']/array/value[1]", "1");
        assertXpathEquals(service, "service-properties/entry[@key='longArray']/array/value[2]", "2");
        assertXpathEquals(service, "service-properties/entry[@key='longArray']/array/value[3]", "3");

        assertXpathEquals(service, "count(service-properties/entry[@key='stringArray']/@value)", "0");
        assertXpathEquals(service, "count(service-properties/entry[@key='stringArray']/value)", "0");
        assertXpathEquals(service, "count(service-properties/entry[@key='stringArray']/array/value)", "2");
        assertXpathEquals(service, "count(service-properties/entry[@key='stringArray']/array/@value-type)", "0");
        assertXpathEquals(service, "service-properties/entry[@key='stringArray']/array/value[1]", "a");
        assertXpathEquals(service, "service-properties/entry[@key='stringArray']/array/value[2]", "b");
    }

    @Test
    public void shouldGenerateSimplestServiceFromFactory() throws XPathExpressionException {
        Node service = getServiceByRef("producedSimplestService");
        assertXpathEquals(service, "@auto-export", "interfaces");
        assertXpathDoesNotExist(service, "service-properties");
    }

    @Test
    public void shouldGenerateServiceWithAllClassesFromFactory() throws XPathExpressionException {
        Node service = getServiceByRef("producedServiceWithAllClasses");
        assertXpathEquals(service, "@auto-export", "all-classes");
    }

    @Test
    public void shouldGenerateServiceWithClassHierarchyFromFactory() throws XPathExpressionException {
        Node service = getServiceByRef("producedServiceWithClassHierarchy");
        assertXpathEquals(service, "@auto-export", "class-hierarchy");
    }

    @Test
    public void shouldGenerateServiceWithOneInterfaceFromFactory() throws XPathExpressionException {
        Node service = getServiceByRef("producedServiceWithOneInterface");
        assertXpathEquals(service, "count(@auto-export)", "0");
        assertXpathEquals(service, "count(interfaces)", "0");
        assertXpathEquals(service, "@interface", Service2.class.getName());
    }

    @Test
    public void shouldGenerateServiceWithManyInterfacesFromFactory() throws XPathExpressionException {
        Node service = getServiceByRef("producedServiceWithManyInterfaces");
        assertXpathEquals(service, "count(@auto-export)", "0");
        assertXpathEquals(service, "count(@interface)", "0");
        assertXpathEquals(service, "count(interfaces/value)", "2");
        assertXpathEquals(service, "interfaces/value[1]", Service1.class.getName());
        assertXpathEquals(service, "interfaces/value[2]", Service2.class.getName());
    }

    @Test
    public void shouldGenerateServiceWithRankingFromFactory() throws XPathExpressionException {
        Node service = getServiceByRef("producedServiceWithRanking");
        assertXpathEquals(service, "@ranking", "200");
    }

    @Test
    public void shouldGenerateServiceWithRankingAndPropertyFromFactory() throws XPathExpressionException {
        Node service = getServiceByRef("producedServiceWithRankingAndProperies");
        assertXpathEquals(service, "@ranking", "-9");
        assertXpathEquals(service, "count(service-properties/entry)", "1");
        assertXpathEquals(service, "service-properties/entry[@key='a']/@value", "1");
    }

    @Test
    public void shouldGenerateServiceWithPropertiesFromFactory() throws XPathExpressionException {
        Node service = getServiceByRef("producedServiceWithProperies");
        assertXpathEquals(service, "count(service-properties/entry)", "5");
        assertXpathEquals(service, "service-properties/entry[@key='oneValue']/@value", "test");
        assertXpathEquals(service, "count(service-properties/entry[@key='oneValue']/value)", "0");

        assertXpathEquals(service, "count(service-properties/entry[@key='intValue']/@value)", "0");
        assertXpathEquals(service, "service-properties/entry[@key='intValue']/value/@type", Integer.class.getName());
        assertXpathEquals(service, "service-properties/entry[@key='intValue']/value/text()", "1");

        assertXpathEquals(service, "count(service-properties/entry[@key='longArray']/@value)", "0");
        assertXpathEquals(service, "count(service-properties/entry[@key='longArray']/value)", "0");
        assertXpathEquals(service, "count(service-properties/entry[@key='longArray']/array/value)", "3");
        assertXpathEquals(service, "service-properties/entry[@key='longArray']/array/@value-type", Long.class.getName());
        assertXpathEquals(service, "service-properties/entry[@key='longArray']/array/value[1]", "1");
        assertXpathEquals(service, "service-properties/entry[@key='longArray']/array/value[2]", "2");
        assertXpathEquals(service, "service-properties/entry[@key='longArray']/array/value[3]", "3");

        assertXpathEquals(service, "count(service-properties/entry[@key='stringArray']/@value)", "0");
        assertXpathEquals(service, "count(service-properties/entry[@key='stringArray']/value)", "0");
        assertXpathEquals(service, "count(service-properties/entry[@key='stringArray']/array/value)", "2");
        assertXpathEquals(service, "count(service-properties/entry[@key='stringArray']/array/@value-type)", "0");
        assertXpathEquals(service, "service-properties/entry[@key='stringArray']/array/value[1]", "a");
        assertXpathEquals(service, "service-properties/entry[@key='stringArray']/array/value[2]", "b");

        assertXpathEquals(service, "count(@ranking)", "0");
        assertXpathEquals(service, "count(service-properties/entry[@key='service.ranking']/@value)", "0");
        assertXpathEquals(service, "service-properties/entry[@key='service.ranking']/value/@type", Integer.class.getName());
        assertXpathEquals(service, "service-properties/entry[@key='service.ranking']/value/text()", "5");
    }

    @Test
    public void shouldInjectReferenceListViaField() throws Exception {
        Node bean = getBeanById("beanWithReferenceLists");
        assertXpathEquals(bean, "property[@name='ref1Field']/@ref", "listOf-ref1");
        assertXpathEquals(bean, "property[@name='myRef1Field']/@ref", "myRef1List");
        assertXpathEquals(bean, "property[@name='myRef1FieldAllProps']/@ref", "listOf-ref1-a453-r1-optional");
        assertXpathEquals(bean, "property[@name='myRef1FieldFilter']/@ref", "listOf-ref1-x1---reference");
    }

    @Test
    public void shouldGenerateReferenceListFromBeanField() throws Exception {
        Node ref1 = getReferenceListById("listOf-ref1");
        assertXpathEquals(ref1, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1, "count(@member-type)", "0");

        Node myRef1 = getReferenceListById("myRef1List");
        assertXpathEquals(myRef1, "@interface", Ref1.class.getName());

        Node ref1a453r1 = getReferenceListById("listOf-ref1-a453-r1-optional");
        assertXpathEquals(ref1a453r1, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1a453r1, "@component-name", "r1");
        assertXpathEquals(ref1a453r1, "@filter", "(a=453)");
        assertXpathEquals(ref1a453r1, "@availability", "optional");

        Node ref1x1 = getReferenceListById("listOf-ref1-x1---reference");
        assertXpathEquals(ref1x1, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1x1, "@filter", "(x=1)");
        assertXpathEquals(ref1x1, "count(@component-name)", "0");
        assertXpathEquals(ref1x1, "@member-type", "service-reference");
    }

    @Test
    public void shouldInjectReferenceListViaSetter() throws Exception {
        Node bean = getBeanById("beanWithReferenceLists");
        assertXpathEquals(bean, "property[@name='ref2Setter']/@ref", "listOf-ref2");
        assertXpathEquals(bean, "property[@name='ref2SetterNamed']/@ref", "myRef2List");
        assertXpathEquals(bean, "property[@name='ref2SetterFull']/@ref", "listOf-ref2-b453-r2-optional");
        assertXpathEquals(bean, "property[@name='ref2SetterComponent']/@ref", "listOf-ref2--blablabla--reference");
    }

    @Test
    public void shouldGenerateReferenceListFromBeanSetter() throws Exception {
        Node ref2 = getReferenceListById("listOf-ref2");
        assertXpathEquals(ref2, "@interface", Ref2.class.getName());
        assertXpathEquals(ref2, "count(@member-type)", "0");

        Node myRef2 = getReferenceListById("myRef2List");
        assertXpathEquals(myRef2, "@interface", Ref2.class.getName());

        Node ref1b453r2 = getReferenceListById("listOf-ref2-b453-r2-optional");
        assertXpathEquals(ref1b453r2, "@interface", Ref2.class.getName());
        assertXpathEquals(ref1b453r2, "@component-name", "r2");
        assertXpathEquals(ref1b453r2, "@filter", "(b=453)");
        assertXpathEquals(ref1b453r2, "@availability", "optional");

        Node ref2blablabla = getReferenceListById("listOf-ref2--blablabla--reference");
        assertXpathEquals(ref2blablabla, "@interface", Ref2.class.getName());
        assertXpathEquals(ref2blablabla, "@component-name", "blablabla");
        assertXpathEquals(ref2blablabla, "count(@filter)", "0");
        assertXpathEquals(ref2blablabla, "@member-type", "service-reference");
    }

    @Test
    public void shouldInjectReferenceListViaConstructor() throws Exception {
        Node bean = getBeanById("beanWithReferenceLists");
        assertXpathEquals(bean, "argument[1]/@ref", "listOf-ref1");
        assertXpathEquals(bean, "argument[2]/@ref", "listOf-ref2---optional-reference");
        assertXpathEquals(bean, "argument[3]/@ref", "listOf-ref1-y3");
        assertXpathEquals(bean, "argument[4]/@ref", "listOf-ref1--compForConstr");
        assertXpathEquals(bean, "argument[5]/@ref", "listOf-ref1-y3-compForConstr");
        assertXpathEquals(bean, "argument[6]/@ref", "ref1ListForCons");
    }

    @Test
    public void shouldGenerateReferenceListFromBeanConstructor() throws Exception {
        Node ref1 = getReferenceListById("listOf-ref1");
        assertXpathEquals(ref1, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1, "count(@member-type)", "0");

        Node ref2optional20000 = getReferenceListById("listOf-ref2---optional-reference");
        assertXpathEquals(ref2optional20000, "@interface", Ref2.class.getName());
        assertXpathEquals(ref2optional20000, "@availability", "optional");
        assertXpathEquals(ref2optional20000, "@member-type", "service-reference");

        Node ref1y3 = getReferenceListById("listOf-ref1-y3");
        assertXpathEquals(ref1y3, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1y3, "count(@component-name)", "0");
        assertXpathEquals(ref1y3, "@filter", "(y=3)");

        Node ref1compForConstr = getReferenceListById("listOf-ref1--compForConstr");
        assertXpathEquals(ref1compForConstr, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1compForConstr, "@component-name", "compForConstr");
        assertXpathEquals(ref1compForConstr, "count(@filter)", "0");

        Node ref1y3compForConstr = getReferenceListById("listOf-ref1-y3-compForConstr");
        assertXpathEquals(ref1y3compForConstr, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1y3compForConstr, "@component-name", "compForConstr");
        assertXpathEquals(ref1y3compForConstr, "@filter", "(y=3)");

        Node ref1ForCons = getReferenceListById("ref1ListForCons");
        assertXpathEquals(ref1ForCons, "@interface", Ref1.class.getName());
        assertXpathEquals(ref1ForCons, "@availability", "optional");
    }

    @Test
    public void shouldInjectReferenceListToProducedBean() throws Exception {
        Node bean = getBeanById("producedWithReferenceLists");
        assertXpathEquals(bean, "argument[1]/@ref", "listOf-ref3");
        assertXpathEquals(bean, "argument[2]/@ref", "listOf-ref4---optional");
        assertXpathEquals(bean, "argument[3]/@ref", "listOf-ref3-y3");
        assertXpathEquals(bean, "argument[4]/@ref", "listOf-ref3--compForProduces");
        assertXpathEquals(bean, "argument[5]/@ref", "listOf-ref3-y3-compForProduces--reference");
        assertXpathEquals(bean, "argument[6]/@ref", "ref3ListForProduces");
    }

    @Test
    public void shouldGenerateReferenceListFromProducedBean() throws Exception {
        Node ref3 = getReferenceListById("listOf-ref3");
        assertXpathEquals(ref3, "@interface", Ref3.class.getName());
        assertXpathEquals(ref3, "count(@member-type)", "0");

        Node ref4optional = getReferenceListById("listOf-ref4---optional");
        assertXpathEquals(ref4optional, "@interface", Ref4.class.getName());
        assertXpathEquals(ref4optional, "@availability", "optional");

        Node ref3y3 = getReferenceListById("listOf-ref3-y3");
        assertXpathEquals(ref3y3, "@interface", Ref3.class.getName());
        assertXpathEquals(ref3y3, "count(@component-name)", "0");
        assertXpathEquals(ref3y3, "@filter", "(y=3)");

        Node ref3compForProduces = getReferenceListById("listOf-ref3--compForProduces");
        assertXpathEquals(ref3compForProduces, "@interface", Ref3.class.getName());
        assertXpathEquals(ref3compForProduces, "@component-name", "compForProduces");
        assertXpathEquals(ref3compForProduces, "count(@filter)", "0");

        Node ref3y3compForProduces = getReferenceListById("listOf-ref3-y3-compForProduces--reference");
        assertXpathEquals(ref3y3compForProduces, "@interface", Ref3.class.getName());
        assertXpathEquals(ref3y3compForProduces, "@component-name", "compForProduces");
        assertXpathEquals(ref3y3compForProduces, "@filter", "(y=3)");
        assertXpathEquals(ref3y3compForProduces, "@member-type", "service-reference");

        Node ref1ForCons = getReferenceListById("ref3ListForProduces");
        assertXpathEquals(ref1ForCons, "@interface", Ref3.class.getName());
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

    private static Node getTypeConverters() throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/type-converters", document, XPathConstants.NODE);
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

    private static Node getPropertyPlaceholderByPersistentId(String id) throws XPathExpressionException {
        return (Node) xpath.evaluate("/blueprint/property-placeholder[@persistent-id='" + id + "']", document, XPathConstants.NODE);
    }
}
