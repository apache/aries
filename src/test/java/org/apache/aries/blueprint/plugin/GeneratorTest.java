/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin;

import static java.util.Arrays.asList;
import static org.apache.aries.blueprint.plugin.FilteredClassFinder.findClasses;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.aries.blueprint.plugin.model.Context;
import org.apache.aries.blueprint.plugin.model.TransactionalDef;
import org.apache.aries.blueprint.plugin.test.MyBean1;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xbean.finder.ClassFinder;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.Sets;

public class GeneratorTest {

    private XPath xpath;
    private Document document;

    @Test
    public void testGenerate() throws Exception {
        ClassFinder classFinder = new ClassFinder(this.getClass().getClassLoader());
        String packageName = MyBean1.class.getPackage().getName();
        Set<Class<?>> beanClasses = findClasses(classFinder, asList(packageName));
        Context context = new Context(beanClasses);
        context.resolve();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Set<String> namespaces = new HashSet<String>(Arrays.asList(Generator.NS_JPA, Generator.NS_TX));
        new Generator(context, os, namespaces).generate();
        System.out.println(os.toString("UTF-8"));

        document = readToDocument(os);
        xpath = XPathFactory.newInstance().newXPath();
        //xpath.setNamespaceContext(new NameSpaces(document));
        Node bean1 = (Node) xpath.evaluate("/blueprint/bean[@id='myBean1']", document, XPathConstants.NODE);

        // Bean
        Assert.assertEquals(MyBean1.class.getName(), xpath.evaluate("@class", bean1));
        Assert.assertEquals("init", xpath.evaluate("@init-method", bean1));
        Assert.assertEquals("destroy", xpath.evaluate("@destroy-method", bean1));
        Assert.assertEquals("true", xpath.evaluate("@field-injection", bean1));
        Assert.assertEquals("", xpath.evaluate("@scope", bean1));

        // @Transactional
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
        Assert.assertEquals(expectedDefs, defs);

        // @PersistenceContext
        Assert.assertEquals("person", xpath.evaluate("context/@unitname", bean1));
        Assert.assertEquals("em", xpath.evaluate("context/@property", bean1));

        // @PersistenceUnit
        Assert.assertEquals("person", xpath.evaluate("unit/@unitname", bean1));
        Assert.assertEquals("emf", xpath.evaluate("unit/@property", bean1));

        // @Autowired
        Assert.assertEquals("my1", xpath.evaluate("property[@name='bean2']/@ref", bean1));

    }

    private Document readToDocument(ByteArrayOutputStream os) throws ParserConfigurationException,
        SAXException, IOException {
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        return builder.parse(is);
    }

}
