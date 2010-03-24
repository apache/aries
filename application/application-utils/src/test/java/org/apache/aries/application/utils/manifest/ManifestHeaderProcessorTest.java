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

package org.apache.aries.application.utils.manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.VersionRange;
import org.apache.aries.application.utils.internal.MessageUtil;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValuePair;
import org.junit.Test;

public class ManifestHeaderProcessorTest
{
  @Test
  public void testNameValuePair() throws Exception {
    NameValuePair<String, String> nvp = new NameValuePair<String, String>("key", "value");
    assertEquals("The name value pair is not set properly.", nvp.getName(), "key");
    assertEquals("The value is not set properly.", nvp.getValue(), "value");
    NameValuePair<String, String> anotherNvp = new NameValuePair<String, String>("key", "value");
    assertEquals("The two objects of NameValuePair is not equal.", nvp, anotherNvp);
    nvp.setName("newKey");
    nvp.setValue("newValue");
    assertEquals("The name value pair is not set properly.", nvp.getName(), "newKey");
    assertEquals("The value is not set properly.", nvp.getValue(), "newValue");
    
    NameValueMap<String,String> nvm1 = new NameValueMap<String,String>();
    nvm1.put("a","b");
    nvm1.put("c","d");
    NameValueMap<String,String> nvm2 = new NameValueMap<String,String>();
    nvm2.put("c","d");
    nvm2.put("a","b");
    assertEquals("The maps are not equal.", nvm1, nvm2);
    nvm2.put("e","f");
    assertNotSame("The maps are the same.", nvm1, nvm2);
    
    NameValuePair<String, NameValueMap<String,String>> nvp1 = new NameValuePair<String,NameValueMap<String,String>>("one",nvm1);
    NameValuePair<String, NameValueMap<String,String>> nvp2 = new NameValuePair<String,NameValueMap<String,String>>("one",nvm2);
    
    assertNotSame("The pairs are identical ",nvp1,nvp2);
    nvm1.put("e","f");
    assertEquals("The pairs are not equal.", nvp1,nvp2);
    
    List<NameValuePair<String, NameValueMap<String,String>>> bundleInfoList1 = new ArrayList<NameValuePair<String, NameValueMap<String,String>>>();
    bundleInfoList1.add(nvp1);

    List<NameValuePair<String, NameValueMap<String,String>>> bundleInfoList2 = new ArrayList<NameValuePair<String, NameValueMap<String,String>>>();
    bundleInfoList2.add(nvp1);
    
    bundleInfoList1.removeAll(bundleInfoList2);
    assertEquals("The List should be empty", bundleInfoList1.isEmpty(), true);
   
   
    assertNotSame("The two objects of NameValuePair is not equal.", nvp, anotherNvp);
  }

  
  /**
   * Test the Bundle manifest header entry of
   * Bundle-SymbolicName: com.acme.foo;singleton:=true
   */
  @Test
  public void testParseBundleSymbolicName() 
  {
    String bundleSymbolicNameEntry = "com.acme.foo;singleton:=true;fragment-attachment:=always";
    NameValuePair<String, NameValueMap<String, String>> nvp = ManifestHeaderProcessor.parseBundleSymbolicName(bundleSymbolicNameEntry);
    assertEquals("The symbolic name is wrong.", nvp.getName(), "com.acme.foo");
    assertEquals("The value is wrong.", "true", nvp.getValue().get("singleton:") );
    assertEquals("The directive is wrong.", "always", nvp.getValue().get("fragment-attachment:") );
  
    String bundleSymbolicNameEntry2 = "com.acme.foo";
    NameValuePair<String, NameValueMap<String, String>> nvp2 = ManifestHeaderProcessor.parseBundleSymbolicName(bundleSymbolicNameEntry2);
    assertEquals("The symbolic name is wrong.", nvp2.getName(), "com.acme.foo");
    
    
  
  
  }
  
 

  /**
   * Test the import package and import service
   * Import-Package: com.acme.foo;come.acm,e.bar;version="[1.23,1.24.5]";resolution:=mandatory
   */
  @Test
  public void testParseImportString()
  {
    String importPackage = "com.acme.foo,come.acm.e.bar;version=\"[1.23,1.24.5]\";resolution:=mandatory;company=\"ACME\",a.b.c;version=1.2.3;company=com";
  
    Map<String, NameValueMap<String, String>> importPackageReturn = ManifestHeaderProcessor.parseImportString(importPackage);
  
    assertTrue("The package is not set.", importPackageReturn.containsKey("com.acme.foo"));
    assertTrue("The package is not set.", importPackageReturn.containsKey("come.acm.e.bar"));
    assertTrue("The package is not set.", importPackageReturn.containsKey("come.acm.e.bar"));
    assertTrue("The package is not set.", importPackageReturn.containsKey("a.b.c"));
    assertTrue("The package should not contain any attributes.", importPackageReturn.get("com.acme.foo").isEmpty());
    assertEquals("The directive is not set correctly.", "[1.23,1.24.5]", importPackageReturn.get("come.acm.e.bar").get("version"));
    assertEquals("The directive is not set correctly.", "mandatory", importPackageReturn.get("come.acm.e.bar").get("resolution:"));
    assertEquals("The directive is not set correctly.", "ACME", importPackageReturn.get("come.acm.e.bar").get("company"));
    assertEquals("The directive is not set correctly.", "1.2.3", importPackageReturn.get("a.b.c").get("version"));
    assertEquals("The directive is not set correctly.", "com", importPackageReturn.get("a.b.c").get("company"));
    
    importPackage="com.acme.foo";
    
    assertTrue("The package is not set.", importPackageReturn.containsKey("com.acme.foo"));
    assertTrue("The package should not contain any attributes.", importPackageReturn.get("com.acme.foo").isEmpty());
    
    importPackage="com.acme.foo;com.acme.bar;version=2";
    Map<String, NameValueMap<String, String>> importPackageReturn2 = ManifestHeaderProcessor.parseImportString(importPackage);
    assertTrue("The package is not set.", importPackageReturn2.containsKey("com.acme.foo"));
    assertTrue("The package is not set.", importPackageReturn2.containsKey("com.acme.bar"));
    assertEquals("The directive is not set correctly.", "2", importPackageReturn2.get("com.acme.foo").get("version"));
    assertEquals("The directive is not set correctly.", "2", importPackageReturn2.get("com.acme.bar").get("version"));
  }
  @Test
  public void testParseExportString()
  {
    String exportPackage = "com.acme.foo,com.acme.bar;version=1,com.acme.bar;version=2;uses:=\"a.b.c,d.e.f\";security=false;mandatory:=security";
  
    List<NameValuePair<String, NameValueMap<String, String>>> exportPackageReturn = ManifestHeaderProcessor.parseExportString(exportPackage);
    
    int i =0;
    assertEquals("The number of the packages is wrong.", 3, exportPackageReturn.size());
    for (NameValuePair<String, NameValueMap<String, String>> nvp : exportPackageReturn) {
      if (nvp.getName().equals("com.acme.foo")) {
        i++;
        
        assertTrue("The directive or attribute should not be set.", nvp.getValue().isEmpty() );
      } else if ((nvp.getName().equals("com.acme.bar")) && ("2".equals(nvp.getValue().get("version")))) {
      
        
        i++;
        assertEquals("The directive is wrong.", "a.b.c,d.e.f", nvp.getValue().get("uses:"));
        assertEquals("The directive is wrong.", "false", nvp.getValue().get("security"));
        assertEquals("The directive is wrong.", "security", nvp.getValue().get("mandatory:"));
      } else if ((nvp.getName().equals("com.acme.bar")) && ("1".equals(nvp.getValue().get("version")))) {
        i++;
        
        assertNull("The directive is wrong.", nvp.getValue().get("uses:"));
        assertNull("The directive is wrong.", nvp.getValue().get("security"));
        assertNull("The directive is wrong.", nvp.getValue().get("mandatory:"));
      }
    }
    // make sure all three packages stored
    assertEquals("The names of the packages are wrong.", 3, i);
    
    exportPackage = "com.acme.foo";
    
    exportPackageReturn = ManifestHeaderProcessor.parseExportString(exportPackage);
    
    int k =0;
    assertEquals("The number of the packages is wrong.", 1, exportPackageReturn.size());
    for (NameValuePair<String, NameValueMap<String, String>> nvp : exportPackageReturn) {
      if (nvp.getName().equals("com.acme.foo")) {
        k++;
        
        assertTrue("The directive or attribute should not be set.", nvp.getValue().isEmpty() );
      } 
    }
    assertEquals("The names of the packages are wrong.", 1, k);
    
    // test multiple packages separated by ;
    
    exportPackage = "com.acme.foo;com.acme.bar;version=\"2\";resolution:=optional";
    
    exportPackageReturn = ManifestHeaderProcessor.parseExportString(exportPackage);
    
    k =0;
    assertEquals("The number of the packages is wrong.", 2, exportPackageReturn.size());
    for (NameValuePair<String, NameValueMap<String, String>> nvp : exportPackageReturn) {
      if (nvp.getName().equals("com.acme.foo")) {
        k++;
        
        assertEquals("The attribute is wrong.", "2", nvp.getValue().get("version") );
        assertEquals("The attribute is wrong.", "optional", nvp.getValue().get("resolution:"));
      } else if (nvp.getName().equals("com.acme.bar")) {
        k++;
        
        assertEquals("The attribute is wrong.", "2", nvp.getValue().get("version") );
        assertEquals("The attribute is wrong.", "optional", nvp.getValue().get("resolution:"));
      }
    }
    assertEquals("The names of the packages are wrong.", 2, k);
    
    
    
    }
    
    @Test
    public void testExportMandatoryAttributes() {
      String exportPackage = "com.acme.foo,com.acme.bar;version=2;company=dodo;security=false;mandatory:=\"security,company\"";
      
      List<NameValuePair<String, NameValueMap<String, String>>> exportPackageReturn = ManifestHeaderProcessor.parseExportString(exportPackage);
      
      int i =0;
      assertEquals("The number of the packages is wrong.", 2, exportPackageReturn.size());
      for (NameValuePair<String, NameValueMap<String, String>> nvp : exportPackageReturn) {
        if (nvp.getName().equals("com.acme.foo")) {
          i++;
          
          assertTrue("The directive or attribute should not be set.", nvp.getValue().isEmpty() );
        } else if ((nvp.getName().equals("com.acme.bar")) && ("2".equals(nvp.getValue().get("version")))) {
        
          
          i++;
          assertEquals("The directive is wrong.", "dodo", nvp.getValue().get("company"));
          assertEquals("The directive is wrong.", "false", nvp.getValue().get("security"));
          assertEquals("The directive is wrong.", "security,company", nvp.getValue().get("mandatory:"));
        } 
      }
      // make sure all three packages stored
      assertEquals("The names of the packages are wrong.", 2, i);
      
    }
    
    private String createExpectedFilter(NameValueMap<String, String> values, String ... parts)
    {
      StringBuilder builder = new StringBuilder(parts[0]);
      
      for (Map.Entry<String, String> entry : values.entrySet()) {
        if ("version".equals(entry.getKey())) builder.append(parts[2]);
        else if ("company".equals(entry.getKey())) builder.append(parts[1]);
      }
      
      builder.append(parts[3]);
      
      return builder.toString();
    }
    
    /**
     * Test the filter generated correctly
     * @throws Exception
     */
    @Test
    public void testGenerateFilter() throws Exception {
      NameValueMap<String, String> valueMap = new NameValueMap<String, String>();
      valueMap.addToCollection("version", "[1.2, 2.3]");
      valueMap.addToCollection("resulution:", "mandatory");
      valueMap.addToCollection("company", "com");
      String filter = ManifestHeaderProcessor.generateFilter("symbolic-name", "com.ibm.foo", valueMap);
      String expected = createExpectedFilter(valueMap, "(&(symbolic-name=com.ibm.foo)", "(company=com)", "(version>=1.2.0)(version<=2.3.0)", "(mandatory:<*company))");
      assertEquals("The filter is wrong.", expected, filter );
      
      
      valueMap.clear();
      
      valueMap.addToCollection("version", "(1.2, 2.3]");
      valueMap.addToCollection("resulution:", "mandatory");
      valueMap.addToCollection("company", "com");
      filter = ManifestHeaderProcessor.generateFilter("symbolic-name", "com.ibm.foo", valueMap);
      expected = createExpectedFilter(valueMap, "(&(symbolic-name=com.ibm.foo)", "(company=com)", "(version>=1.2.0)(version<=2.3.0)(!(version=1.2.0))", "(mandatory:<*company))");
      assertEquals("The filter is wrong.", expected, filter );
      
      valueMap.clear();
      
      valueMap.addToCollection("version", "(1.2, 2.3)");
      valueMap.addToCollection("resulution:", "mandatory");
      valueMap.addToCollection("company", "com");
      filter = ManifestHeaderProcessor.generateFilter("symbolic-name", "com.ibm.foo", valueMap);
      expected = createExpectedFilter(valueMap, "(&(symbolic-name=com.ibm.foo)", "(company=com)", "(version>=1.2.0)(version<=2.3.0)(!(version=1.2.0))(!(version=2.3.0))", "(mandatory:<*company))");
      assertEquals("The filter is wrong.", expected, filter );
      
      valueMap.clear();
      
      valueMap.addToCollection("version", "1.2");
      valueMap.addToCollection("resulution:", "mandatory");
      valueMap.addToCollection("company", "com");
      filter = ManifestHeaderProcessor.generateFilter("symbolic-name", "com.ibm.foo", valueMap);
      expected = createExpectedFilter(valueMap, "(&(symbolic-name=com.ibm.foo)", "(company=com)", "(version>=1.2.0)", "(mandatory:<*company))");
      assertEquals("The filter is wrong.", expected, filter );
      
      valueMap.clear();
      
      valueMap.addToCollection("resulution:", "mandatory");
      valueMap.addToCollection("company", "com");
      filter = ManifestHeaderProcessor.generateFilter("symbolic-name", "com.ibm.foo", valueMap);
      expected = createExpectedFilter(valueMap, "(&(symbolic-name=com.ibm.foo)", "(company=com)", "", "(mandatory:<*company))");
      assertEquals("The filter is wrong.", expected, filter );
    }
    
    /**
     * Test the version range created correctly
     * @throws Exception
     */
    
    @Test
    public void testVersionRange() throws Exception {
      String version1 = "[1.2.3, 4.5.6]";
      String version2="(1, 2]";
      String version3="[2,4)";
      String version4="(1,2)";
      String version5="2";
      String version6 = "2.3";
      String version7="[1.2.3.q, 2.3.4.p)";
      String version8="1.2.2.5";
      String version9="a.b.c";
      String version10=null;
      String version11="";
      String version12="\"[1.2.3, 4.5.6]\"";
      
      VersionRange vr = ManifestHeaderProcessor.parseVersionRange(version1);
      assertEquals("The value is wrong", "1.2.3", vr.getMinimumVersion().toString());
      assertFalse("The value is wrong", vr.isMinimumExclusive());
      assertEquals("The value is wrong", "4.5.6", vr.getMaximumVersion().toString());
      assertFalse("The value is wrong", vr.isMaximumExclusive());
      
      vr = ManifestHeaderProcessor.parseVersionRange(version2);
      assertEquals("The value is wrong", "1.0.0", vr.getMinimumVersion().toString());
      assertTrue("The value is wrong", vr.isMinimumExclusive());
      assertEquals("The value is wrong", "2.0.0", vr.getMaximumVersion().toString());
      assertFalse("The value is wrong", vr.isMaximumExclusive());
      
      vr = ManifestHeaderProcessor.parseVersionRange(version3);
      
      assertEquals("The value is wrong", "2.0.0", vr.getMinimumVersion().toString());
      assertFalse("The value is wrong", vr.isMinimumExclusive());
      assertEquals("The value is wrong", "4.0.0", vr.getMaximumVersion().toString());
      assertTrue("The value is wrong", vr.isMaximumExclusive());
      
      vr = ManifestHeaderProcessor.parseVersionRange(version4);
      
      assertEquals("The value is wrong", "1.0.0", vr.getMinimumVersion().toString());
      assertTrue("The value is wrong", vr.isMinimumExclusive());
      assertEquals("The value is wrong", "2.0.0", vr.getMaximumVersion().toString());
      assertTrue("The value is wrong", vr.isMaximumExclusive());
      
      vr = ManifestHeaderProcessor.parseVersionRange(version5);
      assertEquals("The value is wrong", "2.0.0", vr.getMinimumVersion().toString());
      assertFalse("The value is wrong", vr.isMinimumExclusive());
      assertNull("The value is wrong", vr.getMaximumVersion());
      assertFalse("The value is wrong", vr.isMaximumExclusive());
      
      vr = ManifestHeaderProcessor.parseVersionRange(version6);
      assertEquals("The value is wrong", "2.3.0", vr.getMinimumVersion().toString());
      assertFalse("The value is wrong", vr.isMinimumExclusive());
      assertNull("The value is wrong", vr.getMaximumVersion());
      assertFalse("The value is wrong", vr.isMaximumExclusive());
      
      vr = ManifestHeaderProcessor.parseVersionRange(version7);
      assertEquals("The value is wrong", "1.2.3.q", vr.getMinimumVersion().toString());
      assertFalse("The value is wrong", vr.isMinimumExclusive());
      assertEquals("The value is wrong", "2.3.4.p", vr.getMaximumVersion().toString());
      assertTrue("The value is wrong", vr.isMaximumExclusive());
      
      vr = ManifestHeaderProcessor.parseVersionRange(version8);
      assertEquals("The value is wrong", "1.2.2.5", vr.getMinimumVersion().toString());
      assertFalse("The value is wrong", vr.isMinimumExclusive());
      assertNull("The value is wrong", vr.getMaximumVersion());
      assertFalse("The value is wrong", vr.isMaximumExclusive());
      boolean exception = false;
      try {
      vr = ManifestHeaderProcessor.parseVersionRange(version9);
      } catch (Exception e){
        exception = true;
      }
      
      assertTrue("The value is wrong", exception);
      boolean exceptionNull = false;
      try {
        vr = ManifestHeaderProcessor.parseVersionRange(version10);
        } catch (Exception e){
          exceptionNull = true;
        }
        assertTrue("The value is wrong", exceptionNull);
        // empty version should be defaulted to >=0.0.0
        vr = ManifestHeaderProcessor.parseVersionRange(version11);
        assertEquals("The value is wrong", "0.0.0", vr.getMinimumVersion().toString());
        assertFalse("The value is wrong", vr.isMinimumExclusive());
        assertNull("The value is wrong", vr.getMaximumVersion());
        assertFalse("The value is wrong", vr.isMaximumExclusive());
          
          
          vr = ManifestHeaderProcessor.parseVersionRange(version12);
          assertEquals("The value is wrong", "1.2.3", vr.getMinimumVersion().toString());
          assertFalse("The value is wrong", vr.isMinimumExclusive());
          assertEquals("The value is wrong", "4.5.6", vr.getMaximumVersion().toString());
          assertFalse("The value is wrong", vr.isMaximumExclusive());  
    }
    
    @Test
    public void testInvalidVersions() throws Exception
    {
      try {
        ManifestHeaderProcessor.parseVersionRange("a");
        assertTrue("Should have thrown an exception", false);
      } catch (IllegalArgumentException e) {
        // assertEquals(MessageUtil.getMessage("APPUTILS0009E", "a"), e.getMessage());
      }
      
      try {
        ManifestHeaderProcessor.parseVersionRange("[1.0.0,1.0.1]", true);
        assertTrue("Should have thrown an exception", false);
      } catch (IllegalArgumentException e) {
        // assertEquals(MessageUtil.getMessage("APPUTILS0011E", "[1.0.0,1.0.1]"), e.getMessage());
      }

    }

    @Test
    public void testSplit() throws Exception {
      String export = "com.ibm.ws.eba.obr.fep.bundle122;version=\"3\";company=mood;local=yes;security=yes;mandatory:=\"mood,security\"";
      List<String> result = ManifestHeaderProcessor.split(export, ",");
      assertEquals("The result is wrong.", export, result.get(0));
      assertEquals("The result is wrong.", 1, result.size());
      
      String aString = "com.acme.foo;weirdAttr=\"one;two;three\";weirdDir:=\"1;2;3\"";
      result = ManifestHeaderProcessor.split(aString, ";");
      assertEquals("The result is wrong.", "com.acme.foo", result.get(0));
      assertEquals("The result is wrong.", "weirdAttr=\"one;two;three\"", result.get(1));
      assertEquals("The result is wrong.", "weirdDir:=\"1;2;3\"", result.get(2));
      
      assertEquals("The result is wrong.", 3, result.size());
      
      
      
      
      String pkg1 = "com.ibm.ws.eba.example.helloIsolation;version=\"1.0.0\" ";
      String pkg2 = "com.ibm.ws.eba.helloWorldService;version=\"[1.0.0,1.0.0]\"";
      String pkg3 = " com.ibm.ws.eba.helloWorldService;version=\"1.0.0\"";
      String pkg4 = "com.ibm.ws.eba.helloWorldService;version=\"[1.0.0,1.0.0]\";sharing:=shared" ;
      String pkg5 = "com.ibm.ws.eba.helloWorldService;sharing:=shared;version=\"[1.0.0,1.0.0]\"";
      String appContent1 = pkg1 + ", " + pkg2 + ", " + pkg3;
      String appContent2 = pkg2 + ", " + pkg1 + ", " + pkg3;
      String appContent3 = pkg1 + ", " + pkg3 + ", " + pkg2;
      String appContent4 = pkg1 + ", " + pkg3 + ", " + pkg4;
      String appContent5 = pkg1 + ", " + pkg3 + ", " + pkg5;
      
      List<String> splitList = ManifestHeaderProcessor.split(appContent1, ",");
      assertEquals(pkg1.trim(), splitList.get(0));
      assertEquals(pkg2.trim(), splitList.get(1));
      assertEquals(pkg3.trim(), splitList.get(2));
      
      splitList = ManifestHeaderProcessor.split(appContent2, ",");
      assertEquals(pkg2.trim(), splitList.get(0));
      assertEquals(pkg1.trim(), splitList.get(1));
      assertEquals(pkg3.trim(), splitList.get(2));
      
      splitList = ManifestHeaderProcessor.split(appContent3, ",");
      assertEquals(pkg1.trim(), splitList.get(0));
      assertEquals(pkg3.trim(), splitList.get(1));
      assertEquals(pkg2.trim(), splitList.get(2));
      
      splitList = ManifestHeaderProcessor.split(appContent4, ",");
      assertEquals(pkg1.trim(), splitList.get(0));
      assertEquals(pkg3.trim(), splitList.get(1));
      assertEquals(pkg4.trim(), splitList.get(2));
      
      splitList = ManifestHeaderProcessor.split(appContent5, ",");
      assertEquals(pkg1.trim(), splitList.get(0));
      assertEquals(pkg3.trim(), splitList.get(1));
      assertEquals(pkg5.trim(), splitList.get(2));   
    }
    
    @Test
    public void testParseFilter()
    {
      Map<String,String> attrs = ManifestHeaderProcessor.parseFilter("(package=com.ibm.test)");
      assertEquals("com.ibm.test", attrs.get("package"));
      
      attrs = ManifestHeaderProcessor.parseFilter("(&(package=com.ibm.test)(attr=value))");
      assertEquals("com.ibm.test", attrs.get("package"));
      assertEquals("value", attrs.get("attr"));
      assertEquals(2, attrs.size());
      
      attrs = ManifestHeaderProcessor.parseFilter("(&(version>=1.0.0))");
      assertEquals("1.0.0", attrs.get("version"));
      
      attrs = ManifestHeaderProcessor.parseFilter("(&(version>=1.0.0)(version<=2.0.0))");
      assertEquals("[1.0.0,2.0.0]", attrs.get("version"));

      attrs = ManifestHeaderProcessor.parseFilter("(&(version>=1.0.0)(version<=2.0.0)(!(version=1.0.0)))");
      assertEquals("(1.0.0,2.0.0]", attrs.get("version"));

      attrs = ManifestHeaderProcessor.parseFilter("(&(!(version=2.0.0))(!(version=1.0.0))(version>=1.0.0)(version<=2.0.0))");
      assertEquals("(1.0.0,2.0.0)", attrs.get("version"));
    }
    
    @Test
    public void testExactVersion() throws Exception 
    {
      VersionRange vr;
      try {
        vr = ManifestHeaderProcessor.parseVersionRange("[1.0.0, 2.0.0]", true);
        fail("should not get here 1");
      } catch (IllegalArgumentException e) {
        // expected
      }
      
      vr = ManifestHeaderProcessor.parseVersionRange("[1.0.0, 1.0.0]", true);
      assertTrue(vr.isExactVersion());
      
      try {
        vr = ManifestHeaderProcessor.parseVersionRange("(1.0.0, 1.0.0]", true);
        fail("should not get here 2");
      } catch (IllegalArgumentException e) {
        // expected
      }
      
      try {
        vr = ManifestHeaderProcessor.parseVersionRange("[1.0.0, 1.0.0)", true);
        fail("should not get here 3");
      } catch (IllegalArgumentException e) {
        // expected
      }
      
      vr = ManifestHeaderProcessor.parseVersionRange("[1.0.0, 2.0.0]");
      assertFalse(vr.isExactVersion());
      
      vr = ManifestHeaderProcessor.parseVersionRange("[1.0.0, 1.0.0]");
      assertTrue(vr.isExactVersion());
      
      
    }
}
