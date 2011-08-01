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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.aries.ejb.modelling.EJBLocator;
import org.apache.aries.util.io.IOUtils;
import org.junit.Test;

public class EJBLocatorFactoryTest {

  @Test
  public void testGetEJBLocator() {
    EJBLocator locator = EJBLocatorFactory.getEJBLocator();
    
    assertNotNull(locator);
    assertTrue(locator.getClass().getName(), locator instanceof OpenEJBLocator);
  }

  
  @Test
  public void testGetEJBLocatorNoOpenEJB() throws Exception {
    Class<?> elf = new ClassLoader(getClass().getClassLoader()) {

      @Override
      public Class<?> loadClass(String className) throws ClassNotFoundException {
        if(className.startsWith("org.apache.openejb"))
          throw new ClassNotFoundException(className);
        
        if(className.equals(EJBLocatorFactory.class.getName())) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          try {
            IOUtils.copy(getResourceAsStream(className.replace('.', '/') + ".class"), baos);
          } catch (IOException e) {
            throw new ClassNotFoundException(className, e);
          }
          return defineClass(className, baos.toByteArray(), 0, baos.size());
        }
        
        return super.loadClass(className);
      } 
      
    }.loadClass(EJBLocatorFactory.class.getName());
    
    EJBLocator locator = (EJBLocator) elf.getMethod("getEJBLocator").invoke(null);
    
    assertNotNull(locator);
    assertTrue(locator.getClass().getName(), locator instanceof EJBLocationUnavailable);
  }
}
