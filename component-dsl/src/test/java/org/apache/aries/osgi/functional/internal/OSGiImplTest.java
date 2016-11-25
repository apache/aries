/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.osgi.functional.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Carlos Sierra Andrés
 */
public class OSGiImplTest {

	@Test(expected = IllegalArgumentException.class)
	public void testBuildFilterThrowsIllegalArgument() {
		OSGiImpl.buildFilterString(null, null);
	}

	@Test
	public void testBuildFilterWithBothArguments() {
		assertEquals(
			"(&(filter=*)(objectClass=java.lang.String))",
			OSGiImpl.buildFilterString("(filter=*)", String.class));
	}

	@Test
	public void testBuildFilterWithFilterOnly() {
		assertEquals(
			"(filter=*)", OSGiImpl.buildFilterString("(filter=*)", null));
	}

	@Test
	public void testBuildFilterWithClassOnly() {
		assertEquals(
			"(objectClass=java.lang.String)",
			OSGiImpl.buildFilterString(null, String.class));
	}

}
