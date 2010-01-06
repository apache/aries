/*
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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ibm.osgi.jpa.unit.manager.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import mocks.BundleContextMock;
import mocks.BundleMock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.jpa.PersistenceUnitInfoService;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.aries.unittest.mocks.Skeleton;
import com.ibm.osgi.jpa.unit.parsing.JPAHandler;
import com.ibm.osgi.jpa.util.PersistenceLocationData;


public class PersistenceXMLParsingTest
{

  private Bundle persistenceBundle;
  private SAXParser parser;
  
  @Before
  public void init() throws MalformedURLException, SAXException, ParserConfigurationException
  {
    
    persistenceBundle = Skeleton.newMock(new BundleMock("scooby.doo", new Hashtable<String, Object>()), Bundle.class);
    
    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
    
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();

    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    Schema s = schemaFactory.newSchema(new URL("http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd"));

    parserFactory.setSchema(s);
    parserFactory.setNamespaceAware(true);

    parser = parserFactory.newSAXParser();

  }
  
  @After
  public void destroy()
  {
    BundleContextMock.clear();
  }
  
  @Test
  public void testFile1() throws Exception
  {
    InputStream is = null;
    
    try {
      
      URL xml = new File("unittest/resources/file1/META-INF/persistence.xml").toURI().toURL();
      is = xml.openStream();
      
      URL root = new File("unittest/resources/file1").toURI().toURL();
    
      JPAHandler handler = new JPAHandler(new PersistenceLocationData(xml, root, persistenceBundle), "1.0");
      parser.parse(is, handler);
      
      Collection<? extends PersistenceUnitInfoService> puinfos = handler.getPersistenceUnits();
      assertEquals("An incorrect number of persistence units has been returned.", 4, puinfos.size());
      
      List<PersistenceUnitInfoService> units = getList(puinfos);
      
      PersistenceUnitInfoService unit = units.get(0);
      
      assertEquals("The schema version was incorrect", "1.0",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.SCHEMA_VERSION));
      assertEquals("The unit name was incorrect", "alpha",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.UNIT_NAME));
      assertNull("The transaction type was incorrect",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.TRANSACTION_TYPE));
      assertNull("The provider class name was incorrect",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROVIDER_CLASSNAME));
      assertNull("The jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.JTA_DATASOURCE));
      assertNull("The non jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.NON_JTA_DATASOURCE));
      assertNull("One or more mapping files were specified",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MAPPING_FILES));
      assertNull("One or more jar files were specified",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.JAR_FILES));
      assertNull("One or more managed classes were specified",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MANAGED_CLASSES));
      assertNull("We should not exclude any classes",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.EXCLUDE_UNLISTED_CLASSES));
      assertNull("The properties should never be null",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROPERTIES));
      assertNotNull("The persistence.xml location was not specified", unit.getPersistenceXmlLocation());
      assertEquals("The persistence.xml location points at the wrong file",
          new File("unittest/resources/file1/META-INF/persistence.xml").toURI().toURL().toExternalForm(),
          unit.getPersistenceXmlLocation().toExternalForm());
      assertNotNull("The persistence root location was not specified", unit.getPersistenceUnitRoot());
      assertEquals("The persistence root points at the wrong place",
          new File("unittest/resources/file1").toURI().toURL().toExternalForm(),
          unit.getPersistenceUnitRoot().toExternalForm());
      assertSame("The persistence unit was associated with the wrong bundle", persistenceBundle, unit.getDefiningBundle());
      

      unit = units.get(1);
      
      assertEquals("The schema version was incorrect", "1.0",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.SCHEMA_VERSION));
      assertEquals("The unit name was incorrect", "bravo",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.UNIT_NAME));
      assertEquals("The transaction type was incorrect", PersistenceUnitTransactionType.JTA,
          PersistenceUnitTransactionType.valueOf((String)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.TRANSACTION_TYPE)));
      assertEquals("The provider class name was incorrect", "bravo.persistence.provider",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROVIDER_CLASSNAME));
      assertEquals("The jta datasource jndi name was wrong", "bravo/jtaDS",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.JTA_DATASOURCE));
      assertEquals("The non jta datasource jndi name was wrong", "bravo/nonJtaDS",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.NON_JTA_DATASOURCE));
      assertEquals("An incorrect number of mapping files were specified", 2,
          ((Collection)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MAPPING_FILES)).size());
      assertTrue("Incorrect mapping files were listed",
          ((Collection)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MAPPING_FILES)).contains("bravoMappingFile1.xml")
          && ((Collection)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MAPPING_FILES)).contains("bravoMappingFile2.xml"));
      assertEquals("An incorrect number of jar files were specified", 2,
          ((Collection)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.JAR_FILES)).size());
      assertTrue("Incorrect jar URLs were listed", checkJARURLs((Collection<String>)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.JAR_FILES)));
          
      assertEquals("An incorrect number of managed classes were specified", 2,
          ((Collection)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MANAGED_CLASSES)).size());
      assertTrue("Incorrect managed classes were listed",
          ((Collection)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MANAGED_CLASSES)).contains("bravoClass1")
          && ((Collection)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MANAGED_CLASSES)).contains("bravoClass2"));
      
      //In the schema this defaults to false. There is a separate test (testFile1b)
      //for the spec behaviour, which defaults to true
      assertFalse("We should exclude any classes not listed",
          (Boolean)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.EXCLUDE_UNLISTED_CLASSES));
      assertNotNull("The properties should never be null",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROPERTIES));
      assertEquals("The wrong number of properties were specified", 2,
          ((Properties)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROPERTIES)).size());
      assertEquals("The property had the wrong value", "prop.value",
          ((Properties)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROPERTIES)).getProperty("some.prop"));
      assertEquals("The property had the wrong value", "another.prop.value",
          ((Properties)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROPERTIES)).getProperty("some.other.prop"));
      assertNotNull("The persistence.xml location was not specified", unit.getPersistenceXmlLocation());
      assertEquals("The persistence.xml location points at the wrong file",
          new File("unittest/resources/file1/META-INF/persistence.xml").toURI().toURL().toExternalForm(),
          unit.getPersistenceXmlLocation().toExternalForm());
      assertNotNull("The persistence root location was not specified", unit.getPersistenceUnitRoot());
      assertEquals("The persistence root points at the wrong place",
          new File("unittest/resources/file1").toURI().toURL().toExternalForm(),
          unit.getPersistenceUnitRoot().toExternalForm());
      assertSame("The persistence unit was associated with the wrong bundle", persistenceBundle, unit.getDefiningBundle());
      
      
      unit = units.get(2);
      
      assertEquals("The schema version was incorrect", "1.0",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.SCHEMA_VERSION));
      assertEquals("The unit name was incorrect", "charlie",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.UNIT_NAME));
      assertEquals("The transaction type was incorrect", PersistenceUnitTransactionType.RESOURCE_LOCAL,
          PersistenceUnitTransactionType.valueOf((String)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.TRANSACTION_TYPE)));
      assertEquals("The provider class name was incorrect", "charlie.persistence.provider",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROVIDER_CLASSNAME));
      assertNull("The jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.JTA_DATASOURCE));
      assertNull("The non jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.NON_JTA_DATASOURCE));
      assertNull("One or more mapping files were specified",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MAPPING_FILES));
      assertNull("One or more jar files were specified",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.JAR_FILES));
      assertNull("One or more managed classes were specified",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MANAGED_CLASSES));
      assertTrue("We should not exclude any classes",
          (Boolean)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.EXCLUDE_UNLISTED_CLASSES));
     assertNull("The properties should never be null",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROPERTIES));
      assertNotNull("The persistence.xml location was not specified", unit.getPersistenceXmlLocation());
      assertEquals("The persistence.xml location points at the wrong file",
          new File("unittest/resources/file1/META-INF/persistence.xml").toURI().toURL().toExternalForm(),
          unit.getPersistenceXmlLocation().toExternalForm());
      assertNotNull("The persistence root location was not specified", unit.getPersistenceUnitRoot());
      assertEquals("The persistence root points at the wrong place",
          new File("unittest/resources/file1").toURI().toURL().toExternalForm(),
          unit.getPersistenceUnitRoot().toExternalForm());
      assertSame("The persistence unit was associated with the wrong bundle", persistenceBundle, unit.getDefiningBundle());
      
      
      unit = units.get(3);
      
      assertEquals("The schema version was incorrect", "1.0",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.SCHEMA_VERSION));
      assertEquals("The unit name was incorrect", "delta",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.UNIT_NAME));
      assertEquals("The transaction type was incorrect", PersistenceUnitTransactionType.RESOURCE_LOCAL,
          PersistenceUnitTransactionType.valueOf((String)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.TRANSACTION_TYPE)));
      assertEquals("The provider class name was incorrect", "delta.persistence.provider",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROVIDER_CLASSNAME));
      assertNull("The jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.JTA_DATASOURCE));
      assertNull("The non jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.NON_JTA_DATASOURCE));
      assertNull("One or more mapping files were specified",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MAPPING_FILES));
      assertNull("One or more jar files were specified",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.JAR_FILES));
      assertNull("One or more managed classes were specified",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.MANAGED_CLASSES));
      assertFalse("We should not exclude any classes",
          (Boolean)unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.EXCLUDE_UNLISTED_CLASSES));
      assertNull("The properties should never be null",
          unit.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.PROPERTIES));
      assertNotNull("The persistence.xml location was not specified", unit.getPersistenceXmlLocation());
      assertEquals("The persistence.xml location points at the wrong file",
          new File("unittest/resources/file1/META-INF/persistence.xml").toURI().toURL().toExternalForm(),
          unit.getPersistenceXmlLocation().toExternalForm());
      assertNotNull("The persistence root location was not specified", unit.getPersistenceUnitRoot());
      assertEquals("The persistence root points at the wrong place",
          new File("unittest/resources/file1").toURI().toURL().toExternalForm(),
          unit.getPersistenceUnitRoot().toExternalForm());
      assertSame("The persistence unit was associated with the wrong bundle", persistenceBundle, unit.getDefiningBundle());

    } finally {
      
      if(is != null)
        is.close();
    }
    
  }

  @Test
  public void testFile2() throws Exception
  {
    InputStream is = null;

    try {
      URL xml = new File("unittest/resources/file2/META-INF/persistence.xml").toURI().toURL();
      is = xml.openStream();
      
      URL root = new File("unittest/resources/file2").toURI().toURL();
    
      JPAHandler handler = new JPAHandler(new PersistenceLocationData(xml, root, persistenceBundle), "1.0");
      parser.parse(is, handler);
      
      Collection<? extends PersistenceUnitInfoService> puinfos = handler.getPersistenceUnits();
      assertEquals("An incorrect number of persistence units has been returned.", 0, puinfos.size());
    } finally {
      if(is != null)
        is.close();
    }
  }
  
  @Test(expected=SAXParseException.class)
  public void testFile3() throws Exception
  {
    InputStream is = null;

    try {
      URL xml = new File("unittest/resources/file3/META-INF/persistence.xml").toURI().toURL();
      is = xml.openStream();
      
      URL root = new File("unittest/resources/file3").toURI().toURL();
    
      JPAHandler handler = new JPAHandler(new PersistenceLocationData(xml, root, persistenceBundle), "1.0");
      parser.parse(is, handler);

      fail("Parsing should not succeed");
    } finally {
      if(is != null)
        is.close();
    }
  }

  private static List<PersistenceUnitInfoService> getList(Collection<? extends PersistenceUnitInfoService> puinfos)
  {
    List<PersistenceUnitInfoService> list = new ArrayList<PersistenceUnitInfoService>();
    
    list.addAll(puinfos);
    
    Collections.sort(list, new Comparator<PersistenceUnitInfoService>() {

      public int compare(PersistenceUnitInfoService o1, PersistenceUnitInfoService o2)
      {
        return ((String)o1.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.UNIT_NAME))
        .compareTo((String)o2.getPersistenceXmlMetadata().get(PersistenceUnitInfoService.UNIT_NAME));
      }
      
    });
    
    return list;
  }
  
  private boolean checkJARURLs(Collection<String> collection)
  {
    List<String> jars = new ArrayList<String>();
    
    jars.add("bravoJarFile1.jar");
    jars.add("bravoJarFile2.jar");
    
    return collection.containsAll(jars);
  }
}
