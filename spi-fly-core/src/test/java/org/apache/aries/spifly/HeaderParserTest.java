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
package org.apache.aries.spifly;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

public class HeaderParserTest extends TestCase {

    @Test
    public void testMethodWithMultipleParameters() {

        String header = "javax.ws.rs.client.FactoryFinder#find(java.lang.String," +
                "java.lang.String),javax.ws.rs.ext.FactoryFinder#find(java.lang.String,java" +
                ".lang.String) ,javax.ws.rs.other.FactoryFinder#find(java.lang.String,java" +
                ".lang.String)";

        List<HeaderParser.PathElement> pathElements = HeaderParser.parseHeader(header);
        assertEquals(3, pathElements.size());
        assertEquals(pathElements.get(0).getName(), "javax.ws.rs.client.FactoryFinder#find(java.lang.String,java.lang.String)");
        assertEquals(pathElements.get(1).getName(), "javax.ws.rs.ext.FactoryFinder#find(java.lang.String,java.lang.String)");
        assertEquals(pathElements.get(2).getName(), "javax.ws.rs.other.FactoryFinder#find(java.lang.String,java.lang.String)");
    }

}