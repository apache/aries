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
package org.apache.aries.subsystem.core.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class GenericHeaderTest {
	@Test
	public void testEmptyHeader() {
		try {
		    GenericHeader header = new GenericHeader("Foo-Bar", "");
		    assertEquals(
		    		"Empty headers are treated the same as those with an empty quoted string",
		    		"\"\"",
		    		header.getValue());
		    assertEquals("Empty headers should have one clause", 1, header.getClauses().size());
		}
		catch (Exception e) {
		    fail("Empty headers are allowed");
		}
	}
}
