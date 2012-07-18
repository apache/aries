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
package org.apache.aries.jpa.container.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.apache.aries.jpa.container.impl.PersistenceDescriptorImpl;
import org.apache.aries.jpa.container.parsing.impl.PersistenceDescriptorParserImpl;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class PersistenceXMLParsingTest
{

  /**
   * Test parsing a persistence descriptor with several entries
   * @throws Exception
   */
  @Test
  public void testFile1() throws Exception
  {
    InputStream is = null;
    try {
      String location = "file1/META-INF/persistence.xml"; 
      is = getClass().getClassLoader().getResourceAsStream(location);
      PersistenceDescriptor descriptor = new PersistenceDescriptorImpl(location, is);
      
      Bundle b = Skeleton.newMock(Bundle.class);
      
      Collection<? extends ParsedPersistenceUnit> parsedUnits = new PersistenceDescriptorParserImpl().parse(b, descriptor);
      assertEquals("An incorrect number of persistence units has been returned.", 4, parsedUnits.size());
      
      List<ParsedPersistenceUnit> units = getList(parsedUnits);
      
      ParsedPersistenceUnit unit = units.get(0);
      
      assertEquals("The schema version was incorrect", "1.0",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.SCHEMA_VERSION));
      assertEquals("The unit name was incorrect", "alpha",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.UNIT_NAME));
      assertNull("The transaction type was incorrect",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.TRANSACTION_TYPE));
      assertNull("The provider class name was incorrect",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROVIDER_CLASSNAME));
      assertNull("The jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JTA_DATASOURCE));
      assertNull("The non jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.NON_JTA_DATASOURCE));
      assertNull("One or more mapping files were specified",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MAPPING_FILES));
      assertNull("One or more jar files were specified",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JAR_FILES));
      assertNull("One or more managed classes were specified",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES));
      assertNull("We should not exclude any classes",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.EXCLUDE_UNLISTED_CLASSES));
      assertNotNull("The properties should never be null",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES));
      assertSame("The persistence unit was associated with the wrong bundle", b, unit.getDefiningBundle());
      

      unit = units.get(1);
      
      assertEquals("The schema version was incorrect", "1.0",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.SCHEMA_VERSION));
      assertEquals("The unit name was incorrect", "bravo",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.UNIT_NAME));
      assertEquals("The transaction type was incorrect", "JTA",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.TRANSACTION_TYPE));
      assertEquals("The provider class name was incorrect", "bravo.persistence.provider",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROVIDER_CLASSNAME));
      assertEquals("The jta datasource jndi name was wrong", "bravo/jtaDS",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JTA_DATASOURCE));
      assertEquals("The non jta datasource jndi name was wrong", "bravo/nonJtaDS",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.NON_JTA_DATASOURCE));
      assertEquals("An incorrect number of mapping files were specified", 2,
          ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MAPPING_FILES)).size());
      assertTrue("Incorrect mapping files were listed",
          ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MAPPING_FILES)).contains("bravoMappingFile1.xml")
          && ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MAPPING_FILES)).contains("bravoMappingFile2.xml"));
      assertEquals("An incorrect number of jar files were specified", 2,
          ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JAR_FILES)).size());
      assertTrue("Incorrect jar URLs were listed", checkJARURLs((Collection<String>)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JAR_FILES)));
          
      assertEquals("An incorrect number of managed classes were specified", 2,
          ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES)).size());
      assertTrue("Incorrect managed classes were listed",
          ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES)).contains("bravoClass1")
          && ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES)).contains("bravoClass2"));
      
      //In the schema this defaults to false. There is a separate test (testFile1b)
      //for the spec behaviour, which defaults to true
      assertFalse("We should exclude any classes not listed",
          (Boolean)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.EXCLUDE_UNLISTED_CLASSES));
      assertNotNull("The properties should never be null",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES));
      assertEquals("The wrong number of properties were specified", 2,
          ((Properties)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES)).size());
      assertEquals("The property had the wrong value", "prop.value",
          ((Properties)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES)).getProperty("some.prop"));
      assertEquals("The property had the wrong value", "another.prop.value",
          ((Properties)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES)).getProperty("some.other.prop"));
      assertSame("The persistence unit was associated with the wrong bundle", b, unit.getDefiningBundle());
      
      
      unit = units.get(2);
      
      assertEquals("The schema version was incorrect", "1.0",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.SCHEMA_VERSION));
      assertEquals("The unit name was incorrect", "charlie",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.UNIT_NAME));
      assertEquals("The transaction type was incorrect", "RESOURCE_LOCAL",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.TRANSACTION_TYPE));
      assertEquals("The provider class name was incorrect", "charlie.persistence.provider",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROVIDER_CLASSNAME));
      assertNull("The jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JTA_DATASOURCE));
      assertNull("The non jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.NON_JTA_DATASOURCE));
      assertNull("One or more mapping files were specified",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MAPPING_FILES));
      assertNull("One or more jar files were specified",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JAR_FILES));
      assertNull("One or more managed classes were specified",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES));
      assertTrue("We should not exclude any classes",
          (Boolean)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.EXCLUDE_UNLISTED_CLASSES));
      assertNotNull("The properties should never be null",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES));

      assertSame("The persistence unit was associated with the wrong bundle", b, unit.getDefiningBundle());
      
      
      unit = units.get(3);
      
      assertEquals("The schema version was incorrect", "1.0",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.SCHEMA_VERSION));
      assertEquals("The unit name was incorrect", "delta",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.UNIT_NAME));
      assertEquals("The transaction type was incorrect", "RESOURCE_LOCAL",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.TRANSACTION_TYPE));
      assertEquals("The provider class name was incorrect", "delta.persistence.provider",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROVIDER_CLASSNAME));
      assertNull("The jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JTA_DATASOURCE));
      assertNull("The non jta datasource jndi name was wrong",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.NON_JTA_DATASOURCE));
      assertNull("One or more mapping files were specified",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MAPPING_FILES));
      assertNull("One or more jar files were specified",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JAR_FILES));
      assertNull("One or more managed classes were specified",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES));
      assertFalse("We should not exclude any classes",
          (Boolean)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.EXCLUDE_UNLISTED_CLASSES));
      assertNotNull("The properties should never be null",
          unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES));
      assertSame("The persistence unit was associated with the wrong bundle", b, unit.getDefiningBundle());

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
      String location = "file2/META-INF/persistence.xml";
      is = getClass().getClassLoader().getResourceAsStream(location);
      PersistenceDescriptor descriptor = new PersistenceDescriptorImpl(location, is);

      Bundle b = Skeleton.newMock(Bundle.class);
      
      Collection<? extends ParsedPersistenceUnit> parsedUnits = new PersistenceDescriptorParserImpl().parse(b, descriptor);
      assertEquals("An incorrect number of persistence units has been returned.", 0, parsedUnits.size());
    } finally {
      if(is != null)
        is.close();
    }
  }
  
  @Test(expected=PersistenceDescriptorParserException.class)
  public void testFile3() throws Exception
  {
    InputStream is = null;

    try {
      String location = "file3/META-INF/persistence.xml";
      is = getClass().getClassLoader().getResourceAsStream(location);
      PersistenceDescriptor descriptor = new PersistenceDescriptorImpl(location, is);
      
      Bundle b = Skeleton.newMock(Bundle.class);
      
      Collection<? extends ParsedPersistenceUnit> parsedUnits = new PersistenceDescriptorParserImpl().parse(b, descriptor);

      fail("Parsing should not succeed");
    } finally {
      if(is != null)
        is.close();
    }
  }
  
  @Test
  public void testJPA2() throws Exception
  {
    InputStream is = null;
    try {
      String location = "file22/META-INF/persistence.xml";
      is = getClass().getClassLoader().getResourceAsStream(location);
      PersistenceDescriptor descriptor = new PersistenceDescriptorImpl(location, is);
      
      Bundle b = Skeleton.newMock(Bundle.class);
      List<ParsedPersistenceUnit> parsedUnits = getList(new PersistenceDescriptorParserImpl().parse(b, descriptor));
      
      assertEquals(2, parsedUnits.size());
      
      ParsedPersistenceUnit customUnit = parsedUnits.get(0);
      assertEquals("ENABLE_SELECTIVE", customUnit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.SHARED_CACHE_MODE));
      assertEquals("CALLBACK", customUnit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.VALIDATION_MODE));

      
      ParsedPersistenceUnit defaultUnit = parsedUnits.get(1);
      assertNull(defaultUnit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.SHARED_CACHE_MODE));
      assertNull(defaultUnit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.VALIDATION_MODE));
        
        
    } finally {
      if (is != null)
        is.close();
    }
  }
  
  
  /**
   * Test parsing a persistence descriptor with several entries
   * @throws Exception
   */
  @Test
  public void testReallyBigFile() throws Exception
  {
    InputStream is = null;
    try {
      String location = "file24/META-INF/persistence.xml"; 
      is = getClass().getClassLoader().getResourceAsStream(location);
      PersistenceDescriptor descriptor = new PersistenceDescriptorImpl(location, is);
      
      Bundle b = Skeleton.newMock(Bundle.class);
      
      Collection<? extends ParsedPersistenceUnit> parsedUnits = new PersistenceDescriptorParserImpl().parse(b, descriptor);
      assertEquals("An incorrect number of persistence units has been returned.", 33, parsedUnits.size());
      
      List<ParsedPersistenceUnit> units = getList(parsedUnits);
      
      Collections.sort(units, new Comparator<ParsedPersistenceUnit>() {

        public int compare(ParsedPersistenceUnit p1,
            ParsedPersistenceUnit p2) {
          
          return Integer.valueOf((String)p1.getPersistenceXmlMetadata().
              get(ParsedPersistenceUnit.UNIT_NAME)).compareTo(
                  Integer.valueOf((String)p2.getPersistenceXmlMetadata().
                      get(ParsedPersistenceUnit.UNIT_NAME)));
        }
      });
      
      for(int i = 0; i < units.size() ; i++)
        checkParsedUnit(b, units.get(i), Integer.valueOf(i + 1).toString());
      
    } finally {
      if(is != null)
        is.close();
    }
  }
  
  @Test
  public void elementsPrefixedWithPersistenceNameSpaceShouldBeAccepted() throws Exception{
	  InputStream is = null;
	    try {
	      String location = "file26/META-INF/persistence.xml"; 
	      is = getClass().getClassLoader().getResourceAsStream(location);
	      PersistenceDescriptor descriptor = new PersistenceDescriptorImpl(location, is);
	      
	      Bundle b = Skeleton.newMock(Bundle.class);
	      
	      Collection<? extends ParsedPersistenceUnit> parsedUnits = new PersistenceDescriptorParserImpl().parse(b, descriptor);
	      assertEquals("An incorrect number of persistence units has been returned.", 1, parsedUnits.size());
	     
	    } finally {
	      if(is != null)
	        is.close();
	    }
  }
  
  @Test(expected=PersistenceDescriptorParserException.class)
  public void elementsPrefixedWithWrongNameSpaceShouldBeRejected() throws Exception{
	  InputStream is = null;
	    try {
	      String location = "file27/META-INF/persistence.xml"; 
	      is = getClass().getClassLoader().getResourceAsStream(location);
	      PersistenceDescriptor descriptor = new PersistenceDescriptorImpl(location, is);
	      
	      Bundle b = Skeleton.newMock(Bundle.class);
	      
	      Collection<? extends ParsedPersistenceUnit> parsedUnits = new PersistenceDescriptorParserImpl().parse(b, descriptor);
	      fail("should throw");
	     
	    } finally {
	      if(is != null)
	        is.close();
	    }
  }
  
  private void checkParsedUnit(Bundle b, ParsedPersistenceUnit unit, String number) {
    assertEquals("The schema version was incorrect", "1.0",
        unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.SCHEMA_VERSION));
    assertEquals("The unit name was incorrect", number,
        unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.UNIT_NAME));
    assertEquals("The transaction type was incorrect", "JTA",
        unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.TRANSACTION_TYPE));
    assertEquals("The provider class name was incorrect", "provider." + number,
        unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROVIDER_CLASSNAME));
    assertEquals("The jta datasource jndi name was wrong", "jtaDS." + number,
        unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JTA_DATASOURCE));
    assertEquals("The non jta datasource jndi name was wrong", "nonJtaDS." + number,
        unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.NON_JTA_DATASOURCE));
    assertEquals("An incorrect number of mapping files were specified", 1,
        ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MAPPING_FILES)).size());
    assertTrue("Incorrect mapping files were listed",
        ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MAPPING_FILES)).contains("mappingFile." + number));
    assertEquals("An incorrect number of jar files were specified", 1,
        ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JAR_FILES)).size());
    assertTrue("Incorrect jar URLs were listed", ((Collection<String>)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JAR_FILES)).contains("jarFile." + number));
        
    assertEquals("An incorrect number of managed classes were specified", 1,
        ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES)).size());
    assertTrue("Incorrect managed classes were listed",
        ((Collection)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES)).contains("class." + number));
    
    //In the schema this defaults to false. There is a separate test (testFile1b)
    //for the spec behaviour, which defaults to true
    assertFalse("We should exclude any classes not listed",
        (Boolean)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.EXCLUDE_UNLISTED_CLASSES));
    assertNotNull("The properties should never be null",
        unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES));
    assertEquals("The wrong number of properties were specified", 1,
        ((Properties)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES)).size());
    assertEquals("The property had the wrong value", "prop.value." + number,
        ((Properties)unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES)).getProperty("some.prop." + number));
    assertSame("The persistence unit was associated with the wrong bundle", b, unit.getDefiningBundle());
  }

  /**
   * Sort a Collection of ParsedPersistenceUnit into alphabetical order (by unit name)
   * @param puinfos
   * @return
   */
  private static List<ParsedPersistenceUnit> getList(Collection<? extends ParsedPersistenceUnit> puinfos)
  {
    List<ParsedPersistenceUnit> list = new ArrayList<ParsedPersistenceUnit>();
    
    list.addAll(puinfos);
    
    Collections.sort(list, new Comparator<ParsedPersistenceUnit>() {

      public int compare(ParsedPersistenceUnit o1, ParsedPersistenceUnit o2)
      {
        return ((String)o1.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.UNIT_NAME))
        .compareTo((String)o2.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.UNIT_NAME));
      }
      
    });
    
    return list;
  }
  
  /**
   * Check we have all the jar file names we expect
   * @param collection
   * @return
   */
  private boolean checkJARURLs(Collection<String> collection)
  {
    List<String> jars = new ArrayList<String>();
    
    jars.add("bravoJarFile1.jar");
    jars.add("bravoJarFile2.jar");
    
    return collection.containsAll(jars);
  }
}
