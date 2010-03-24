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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jndi.url;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.naming.InvalidNameException;
import javax.naming.NameParser;
import javax.naming.NamingException;

import org.junit.Test;

/**
 * This is where we test the service registry name parser.
 */
public class OsgiNameParserTest
{
  /** The parser we are going to use for testing */
  private NameParser parser = new OsgiNameParser();

  /**
   * OK, so we check that we can call checkNames multiple times.
   * @throws NamingException
   */
  @Test
  public void checkValidNames() throws NamingException
  {
    checkName("aries","services","java.lang.Runnable","(a=b)");
    checkName("aries","services","java.lang.Runnable");
    checkName("osgi","service","java.lang.Runnable");
    checkName("osgi","service","java.lang.Runnable", "(a=b)");
    checkName("osgi","servicelist","java.lang.Runnable");
    checkName("osgi","servicelist","java.lang.Runnable", "(a=b)");
    checkName("osgi","servicelist","jdbc", "grok", "DataSource");
    checkName("osgi", "framework", "bundleContext");
    checkName("osgi","service","javax.sql.DataSource", "(osgi.jndi.servicee.name=jdbc/myDataSource)");
    checkName("osgi","service","javax.sql.DataSource", "(&(a=/b)(c=/d))");
    checkName("osgi", "service");
  }
  
  /**
   * Make sure it fails if we try to parse something that isn't in aries:services
   * @throws NamingException
   */
  @Test(expected=InvalidNameException.class)
  public void checkOutsideNamespace() throws NamingException
  {
    checkName("java","comp","env","jms","cf");
  }
  
  @Test(expected=InvalidNameException.class)
  public void checkIncorrectPath() throws NamingException
  {
    checkName("osgi", "services", "java.lang.Runnable"); 
  }
  
  @Test(expected=InvalidNameException.class)
  public void checkIllegalPath() throws NamingException
  {
    checkName("osgi", "wibble", "java.lang.Runnable"); 
  }
  
  private void checkName(String scheme, String path, String ... elements)
    throws NamingException
  {
    StringBuilder builder = new StringBuilder();
    StringBuilder serviceName = new StringBuilder();
    
    builder.append(scheme);
    builder.append(':');
    builder.append(path);

    if (elements.length > 0) {
      builder.append('/');
      
      for (String element : elements) {
        serviceName.append(element);
        serviceName.append('/');
      }
  
      serviceName.deleteCharAt(serviceName.length() - 1);
      
      builder.append(serviceName);
    }
    
    OsgiName n = (OsgiName) parser.parse(builder.toString());
    
    assertEquals(scheme, n.getScheme());
    assertEquals(path, n.getSchemePath());
    
    if (elements.length > 1) {
      assertEquals(elements[0], n.getInterface());
      if (elements.length == 2) {
        assertTrue("There is no filter in the name", n.hasFilter());
        assertEquals(elements[1], n.getFilter());
      } else assertFalse(n.hasFilter());
    }
    
    if (elements.length == 1) {
      assertFalse("There is a filter in the name", n.hasFilter());
    }
    
    assertEquals(serviceName.toString(), n.getServiceName());
  }
}