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
package org.apache.aries.application.converters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collection;

import org.junit.Test;

public class JSPImportParserTest {

  @Test
  public void testJSPImportParser () throws Exception { 
    InputStream helloImport = getClass().getClassLoader().getResourceAsStream("JSPs/helloImport.jsp");
    Collection<String> imports = JSPImportParser.getImports(helloImport);
    assertTrue("Four imports expected", imports.size() == 4);
    assertTrue(imports.contains("javax.jms"));
    assertTrue(imports.contains("javax.mystuff"));
    assertTrue(imports.contains("javax.transaction"));
    assertTrue(imports.contains("a.b"));
    assertFalse(imports.contains("java.util"));
  }
}
