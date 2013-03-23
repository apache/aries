/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.obr.internal;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Resource;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;

public class FelixCapabilityAdapterTest {
	@Test
	public void testObjectClassAttribute() {
		String objectClass = "com.foo.Bar";
		Capability cap = EasyMock.createNiceMock(Capability.class);
		EasyMock.expect(cap.getName()).andReturn(Capability.SERVICE);
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE.toLowerCase(), objectClass);
		EasyMock.expect(cap.getPropertiesAsMap()).andReturn(props);
		EasyMock.replay(cap);
		FelixCapabilityAdapter adapter = new FelixCapabilityAdapter(cap, EasyMock.createNiceMock(org.osgi.resource.Resource.class));
		assertEquals("Wrong value for attribute " + ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE, objectClass, adapter.getAttributes().get(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE));
	}
	
	@Test
	public void testOsgiServiceNamespace() {
		Capability cap = EasyMock.createNiceMock(Capability.class);
		EasyMock.expect(cap.getName()).andReturn(Capability.SERVICE);
		EasyMock.replay(cap);
		FelixCapabilityAdapter adapter = new FelixCapabilityAdapter(cap, EasyMock.createNiceMock(org.osgi.resource.Resource.class));
		assertEquals("Wrong namespace", ServiceNamespace.SERVICE_NAMESPACE, adapter.getNamespace());
	}
	
	@Test
	public void testOsgiWiringPackageAttribute() {
		String pkg = "com.foo.Bar";
		Capability cap = EasyMock.createNiceMock(Capability.class);
		EasyMock.expect(cap.getName()).andReturn(Capability.PACKAGE).anyTimes();
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(Capability.PACKAGE, pkg);
		EasyMock.expect(cap.getPropertiesAsMap()).andReturn(props);
		EasyMock.replay(cap);
		FelixCapabilityAdapter adapter = new FelixCapabilityAdapter(cap, EasyMock.createNiceMock(org.osgi.resource.Resource.class));
		assertEquals("Wrong value for attribute " + PackageNamespace.PACKAGE_NAMESPACE, pkg, adapter.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
	}
	
	@Test
	public void testOsgiWiringPackageNamespace() {
		Capability cap = EasyMock.createNiceMock(Capability.class);
		EasyMock.expect(cap.getName()).andReturn(Capability.PACKAGE);
		EasyMock.replay(cap);
		FelixCapabilityAdapter adapter = new FelixCapabilityAdapter(cap, EasyMock.createNiceMock(org.osgi.resource.Resource.class));
		assertEquals("Wrong namespace", PackageNamespace.PACKAGE_NAMESPACE, adapter.getNamespace());
	}
	
	@Test
	public void testOsgiWiringBundleNamespace() {
		Capability cap = EasyMock.createNiceMock(Capability.class);
		EasyMock.expect(cap.getName()).andReturn(Capability.BUNDLE);
		EasyMock.replay(cap);
		FelixCapabilityAdapter adapter = new FelixCapabilityAdapter(cap, EasyMock.createNiceMock(org.osgi.resource.Resource.class));
		assertEquals("Wrong namespace", BundleNamespace.BUNDLE_NAMESPACE, adapter.getNamespace());
	}
	
	@Test
	public void testOsgiWiringBundleAttribute() {
		String symbolicName = "derbyclient";
		Capability cap = EasyMock.createNiceMock(Capability.class);
		EasyMock.expect(cap.getName()).andReturn(Capability.BUNDLE).anyTimes();
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(Resource.SYMBOLIC_NAME, symbolicName);
		EasyMock.expect(cap.getPropertiesAsMap()).andReturn(props);
		EasyMock.replay(cap);
		FelixCapabilityAdapter adapter = new FelixCapabilityAdapter(cap, EasyMock.createNiceMock(org.osgi.resource.Resource.class));
		assertEquals("Wrong value for attribute " + BundleNamespace.BUNDLE_NAMESPACE, symbolicName, adapter.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE));
	}
}
