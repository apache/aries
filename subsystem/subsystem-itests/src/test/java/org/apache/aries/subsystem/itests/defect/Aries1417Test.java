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
package org.apache.aries.subsystem.itests.defect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;

public class Aries1417Test extends SubsystemTest {
	@Test
	public void testOsgiImplementation() throws Exception {
		Bundle bundle = getSubsystemCoreBundle();
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		List<Capability> capabilities = revision.getCapabilities(ImplementationNamespace.IMPLEMENTATION_NAMESPACE);
		assertEquals("Wrong capabilities", 1, capabilities.size());
		Capability capability = capabilities.get(0);
		Map<String, Object> attributes = capability.getAttributes();
		assertEquals("Wrong namespace value", "osgi.subsystem", attributes.get(ImplementationNamespace.IMPLEMENTATION_NAMESPACE));
		Object version = attributes.get(ImplementationNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		assertTrue("Wrong version type", version instanceof Version);
		assertEquals("Wrong version", Version.parseVersion("1.1"), version);
		assertEquals("Wrong uses", "org.osgi.service.subsystem", capability.getDirectives().get(ImplementationNamespace.CAPABILITY_USES_DIRECTIVE));
	}
	
	@Test
	public void testOsgiService() throws Exception {
		Bundle bundle = getSubsystemCoreBundle();
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		List<Capability> capabilities = revision.getCapabilities(ServiceNamespace.SERVICE_NAMESPACE);
		assertEquals("Wrong capabilities", 1, capabilities.size());
		Capability capability = capabilities.get(0);
		Map<String, Object> attributes = capability.getAttributes();
		Object objectClass = attributes.get(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE);
		assertTrue("Wrong objectClass type", objectClass instanceof List);
		@SuppressWarnings({ "rawtypes" })
		List objectClassList = (List)objectClass;
		assertEquals("Wrong objectClass size", 2, objectClassList.size());
		Object objectClass1 = objectClassList.get(0);
		assertTrue("Wrong objectClass type", objectClass1 instanceof String);
		assertEquals("Wrong objectClass", "org.osgi.service.subsystem.Subsystem", objectClass1);
		Object objectClass2 = objectClassList.get(1);
		assertTrue("Wrong objectClass type", objectClass2 instanceof String);
		assertEquals("Wrong objectClass", "org.apache.aries.subsystem.AriesSubsystem", objectClass2);
		assertEquals("Wrong uses", "org.osgi.service.subsystem,org.apache.aries.subsystem", capability.getDirectives().get(ServiceNamespace.CAPABILITY_USES_DIRECTIVE));
	}
}
