/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.ejb.modelling.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.aries.application.modelling.ModellerException;
import org.junit.Test;

public class ParsedEJBServicesTest {
  
  @Test
  public void testNoAllowedNames() throws ModellerException {
    ParsedEJBServices pes = new ParsedEJBServices();
    pes.addEJBView("Foo", "Stateless", "com.acme.Bar", false);
    
    assertTrue(pes.getServices().isEmpty());
  }
  
  @Test
  public void testNONE() throws ModellerException {
    ParsedEJBServices pes = new ParsedEJBServices();
    pes.setAllowedNames(Arrays.asList("NONE", "Foo"));
    pes.addEJBView("Foo", "Stateless", "com.acme.Bar", false);
    
    assertTrue(pes.getServices().isEmpty());
  }  
  
  @Test
  public void testALL() throws ModellerException {
    ParsedEJBServices pes = new ParsedEJBServices();
    pes.setAllowedNames(Arrays.asList("ALL", "Foo"));
    pes.addEJBView("Foo", "Stateless", "com.acme.Bar", false);
    pes.addEJBView("Baz", "Stateless", "com.acme.Bar", true);
    
    assertEquals(2, pes.getServices().size());
    
    Iterator it = pes.getServices().iterator();
    assertEquals(new EJBServiceExport("Foo", "Stateless", "com.acme.Bar", false), 
        it.next());
    
    assertEquals(new EJBServiceExport("Baz", "Stateless", "com.acme.Bar", true),
        it.next());
  } 
  
  @Test
  public void testSome() throws ModellerException {
    ParsedEJBServices pes = new ParsedEJBServices();
    pes.setAllowedNames(Arrays.asList("Bar", "Baz"));
    pes.addEJBView("Foo", "Stateless", "com.acme.Bar", false);
    pes.addEJBView("Baz", "Stateless", "com.acme.Bar", true);
    
    assertEquals(1, pes.getServices().size());
    
    Iterator it = pes.getServices().iterator();
    
    assertEquals(new EJBServiceExport("Baz", "Stateless", "com.acme.Bar", true),
        it.next());
  } 

}
