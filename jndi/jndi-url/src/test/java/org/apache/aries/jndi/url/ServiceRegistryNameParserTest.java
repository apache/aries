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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import org.junit.Test;

/**
 * This is where we test the service registry name parser.
 */
public class ServiceRegistryNameParserTest
{
  /** The parser we are going to use for testing */
  private NameParser parser = new ServiceRegistryNameParser();

  /**
   * OK, so we check that we can call checkNames multiple times.
   * @throws NamingException
   */
  @Test
  public void checkValidNames() throws NamingException
  {
    checkName("aries:services/java.lang.Runnable/(a=b)");
    checkName("aries:services/java.lang.Runnable");
  }
  
  /**
   * Make sure it fails if we try to parse something that isn't in aries:services
   * @throws NamingException
   */
  @Test(expected=InvalidNameException.class)
  public void checkOutsideNamespace() throws NamingException
  {
    checkName("java:comp/env/jms/cf");
  }
  
  /**
   * Check that it fails if no interface name is provided.
   * @throws NamingException
   */
  @Test(expected=InvalidNameException.class)
  public void checkMissingInterface() throws NamingException
  {
    checkName("aries:services");
  }
  
  /**
   * Check that it fails if no interface name is provided in a subtly different
   * way from the previous method.
   * @throws NamingException
   */
  @Test(expected=InvalidNameException.class)
  public void checkMissingInterface2() throws NamingException
  {
    checkName("aries:services/");
  }

  /**
   * This method parses the name and then makes sure what was parsed was parsed
   * correctly.
   * 
   * @param name
   * @throws NamingException
   */
  private void checkName(String name) throws NamingException
  {
    Name n = parser.parse(name);
    assertNotNull("We got a null name back, which is not allowed.", n);
    assertEquals("The name's toString does not produce the original value", name, n.toString());
  }
}