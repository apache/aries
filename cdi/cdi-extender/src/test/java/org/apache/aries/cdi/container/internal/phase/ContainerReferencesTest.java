/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.phase;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.apache.aries.cdi.container.internal.container.CheckedCallback;
import org.apache.aries.cdi.container.internal.container.ConfigurationListener;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.ContainerActivator;
import org.apache.aries.cdi.container.internal.model.ContainerComponent;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.apache.aries.cdi.container.test.MockConfiguration;
import org.apache.aries.cdi.container.test.TestUtil;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.ReferencePolicy;
import org.osgi.service.cdi.ReferencePolicyOption;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.ReferenceDTO;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.converter.TypeReference;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.TimeoutException;
import org.osgi.util.tracker.ServiceTracker;

public class ContainerReferencesTest extends BaseCDIBundleTest {

	@Test
	public void reference_tracking() throws Exception {
		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = TestUtil.mockCaSt(bundle);

		MockConfiguration mockConfiguration = new MockConfiguration("foo.config", null);
		mockConfiguration.update(Maps.dict("fiz", "buz"));
		TestUtil.configurations.add(mockConfiguration);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, caTracker, new Logs.Builder(bundle.getBundleContext()).build());

		ConfigurationListener configurationListener = new ConfigurationListener.Builder(containerState
		).component(
			new ContainerComponent.Builder(containerState,
				new ContainerActivator.Builder(containerState, null)
			).template(
				containerState.containerDTO().template.components.get(0)
			).build()
		).build();

		Promise<Boolean> p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.OPEN && op.type == Op.Type.REFERENCES;
			}
		);

		configurationListener.open();

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);
		assertEquals(1, containerDTO.changeCount);
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		p0.timeout(500).getValue();

		ComponentDTO componentDTO = containerDTO.components.stream().filter(
			c -> c.template.type == ComponentType.CONTAINER
		).findFirst().get();

		assertNotNull(componentDTO);
		assertEquals(1, componentDTO.instances.size());

		ComponentInstanceDTO componentInstanceDTO = componentDTO.instances.get(0);

		assertNotNull(componentInstanceDTO);
		assertEquals(6, componentInstanceDTO.references.size());

		// are we currently blocked waiting for those references?

		Promise<Boolean> p1 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.OPEN && op.type == Op.Type.CONTAINER_INSTANCE;
			}
		);

		p1.timeout(200).getFailure();

		List<ReferenceDTO> references = TestUtil.sort(
			componentInstanceDTO.references, (a, b) -> a.template.name.compareTo(b.template.name));

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(0);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.SERVICE, template.collectionType);
			assertEquals(new TypeReference<Provider<Collection<Foo>>>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.MANY, template.maximumCardinality);
			assertEquals(0, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.dynamicFoos", template.name);
			assertEquals(ReferencePolicy.DYNAMIC, template.policy);
			assertEquals(ReferencePolicyOption.RELUCTANT, template.policyOption);
			assertEquals(Foo.class.getName(), template.serviceType);
			assertEquals("", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(0, reference.minimumCardinality);
			assertEquals("(objectClass=" + Foo.class.getName() + ")", reference.targetFilter);
		}

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(1);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.SERVICE, template.collectionType);
			assertEquals(new TypeReference<Foo>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.ONE, template.maximumCardinality);
			assertEquals(1, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.foo", template.name);
			assertEquals(ReferencePolicy.STATIC, template.policy);
			assertEquals(ReferencePolicyOption.GREEDY, template.policyOption);
			assertEquals(Foo.class.getName(), template.serviceType);
			assertEquals("", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(1, reference.minimumCardinality);
			assertEquals("(objectClass=" + Foo.class.getName() + ")", reference.targetFilter);
		}

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(2);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.SERVICE, template.collectionType);
			assertEquals(new TypeReference<Optional<Foo>>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.ONE, template.maximumCardinality);
			assertEquals(0, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.fooOptional", template.name);
			assertEquals(ReferencePolicy.STATIC, template.policy);
			assertEquals(ReferencePolicyOption.RELUCTANT, template.policyOption);
			assertEquals(Foo.class.getName(), template.serviceType);
			assertEquals("", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(0, reference.minimumCardinality);
			assertEquals("(objectClass=" + Foo.class.getName() + ")", reference.targetFilter);
		}

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(3);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.PROPERTIES, template.collectionType);
			assertEquals(new TypeReference<Collection<Map<String, Object>>>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.MANY, template.maximumCardinality);
			assertEquals(0, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.propertiesFoos", template.name);
			assertEquals(ReferencePolicy.STATIC, template.policy);
			assertEquals(ReferencePolicyOption.RELUCTANT, template.policyOption);
			assertEquals(Foo.class.getName(), template.serviceType);
			assertEquals("", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(0, reference.minimumCardinality);
			assertEquals("(objectClass=" + Foo.class.getName() + ")", reference.targetFilter);
		}

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(4);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.REFERENCE, template.collectionType);
			assertEquals(new TypeReference<Collection<ServiceReference<Foo>>>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.MANY, template.maximumCardinality);
			assertEquals(0, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.serviceReferencesFoos", template.name);
			assertEquals(ReferencePolicy.STATIC, template.policy);
			assertEquals(ReferencePolicyOption.RELUCTANT, template.policyOption);
			assertEquals(Foo.class.getName(), template.serviceType);
			assertEquals("(service.scope=prototype)", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(0, reference.minimumCardinality);
			assertEquals("(&(objectClass=" + Foo.class.getName() + ")(service.scope=prototype))", reference.targetFilter);
		}

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(5);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.TUPLE, template.collectionType);
			assertEquals(new TypeReference<Collection<Entry<Map<String, Object>, Integer>>>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.MANY, template.maximumCardinality);
			assertEquals(0, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.tupleIntegers", template.name);
			assertEquals(ReferencePolicy.STATIC, template.policy);
			assertEquals(ReferencePolicyOption.RELUCTANT, template.policyOption);
			assertEquals(Integer.class.getName(), template.serviceType);
			assertEquals("", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(0, reference.minimumCardinality);
			assertEquals("(objectClass=" + Integer.class.getName() + ")", reference.targetFilter);
		}
	}

	@Test
	public void test_S_R_M_U_Service() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(
			CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE,
			Arrays.asList(
				"org.apache.aries.cdi.container.test.beans.Reference_S_R_M_U_Service"
			)
		);

		when(
			bundle.adapt(
				BundleWiring.class).getRequiredWires(
					ExtenderNamespace.EXTENDER_NAMESPACE).get(
						0).getRequirement().getAttributes()
		).thenReturn(attributes);

		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = TestUtil.mockCaSt(bundle);

		MockConfiguration mockConfiguration = new MockConfiguration("foo.config", null);
		mockConfiguration.update(Maps.dict("fiz", "buz"));
		TestUtil.configurations.add(mockConfiguration);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, caTracker, new Logs.Builder(bundle.getBundleContext()).build());

		ContainerComponent containerComponent = new ContainerComponent.Builder(containerState,
			new ContainerActivator.Builder(containerState, null)
		).template(
			containerState.containerDTO().template.components.get(0)
		).build();

		Promise<Boolean> p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.OPEN && op.type == Op.Type.REFERENCES;
			}
		);

		containerComponent.open();

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);
		assertEquals(1, containerDTO.changeCount);
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		assertNotNull(p0.timeout(200).getValue());

		ComponentInstanceDTO containerComponentInstance = containerDTO.components.get(0).instances.get(0);

		List<ExtendedReferenceDTO> unresolvedReferences = containerComponentInstance.references.stream().map(
			r -> (ExtendedReferenceDTO)r
		).filter(
			r -> r.matches.size() < r.minimumCardinality
		).collect(Collectors.toList());

		assertEquals(1, unresolvedReferences.size());

		ExtendedReferenceDTO extendedReferenceDTO = unresolvedReferences.get(0);

		assertTrue(extendedReferenceDTO.matches.isEmpty());
		assertEquals(1, extendedReferenceDTO.minimumCardinality);
		assertNotNull(extendedReferenceDTO.serviceTracker);
		assertEquals("(objectClass=org.apache.aries.cdi.container.test.beans.Foo)", extendedReferenceDTO.targetFilter);
		assertNotNull(extendedReferenceDTO.template);
		assertEquals(MaximumCardinality.ONE, extendedReferenceDTO.template.maximumCardinality);
		assertEquals(1, extendedReferenceDTO.template.minimumCardinality);
		assertEquals("org.apache.aries.cdi.container.test.beans.Reference_S_R_M_U_Service.foo", extendedReferenceDTO.template.name);
		assertEquals(ReferencePolicy.STATIC, extendedReferenceDTO.template.policy);
		assertEquals(ReferencePolicyOption.RELUCTANT, extendedReferenceDTO.template.policyOption);
		assertEquals(Foo.class.getName(), extendedReferenceDTO.template.serviceType);
		assertEquals("", extendedReferenceDTO.template.targetFilter);

		// first test publishing a service targeting one of the optional references

		BundleDTO serviceBundleDTO = new BundleDTO();

		Bundle serviceBundle = TestUtil.mockBundle(serviceBundleDTO, b -> {});

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.OPEN && op.type == Op.Type.CONTAINER_ACTIVATOR;
			}
		);

		ServiceRegistration<Foo> sr1 = serviceBundle.getBundleContext().registerService(
			Foo.class, new Foo() {}, Maps.dict("sr1", "sr1"));

		assertTrue(p0.timeout(200).getValue());

		assertEquals(1, extendedReferenceDTO.matches.size());

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.CLOSE && op.type == Op.Type.CONTAINER_INSTANCE;
			}
		);

		serviceBundle.getBundleContext().registerService(
			Foo.class, new Foo() {}, Maps.dict("sr2", "sr2"));

		assertTrue("should be a TimeoutException", TimeoutException.class.equals(p0.timeout(200).getFailure().getClass()));

		assertEquals(2, extendedReferenceDTO.matches.size());

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.CLOSE && op.type == Op.Type.CONTAINER_INSTANCE;
			}
		);

		Foo foo = new Foo() {};

		ServiceRegistration<Foo> sr3 = serviceBundle.getBundleContext().registerService(
			Foo.class, foo, Maps.dict("sr3", "sr3", Constants.SERVICE_RANKING, 100));

		assertTrue("should be a TimeoutException", TimeoutException.class.equals(p0.timeout(200).getFailure().getClass()));

		assertEquals(3, extendedReferenceDTO.matches.size());

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.CLOSE && op.type == Op.Type.CONTAINER_ACTIVATOR;
			}
		);
		Promise<Boolean> p1 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.OPEN && op.type == Op.Type.CONTAINER_ACTIVATOR;
			}
		);

		sr1.unregister();

		p0.timeout(200).getFailure();

		assertEquals(2, extendedReferenceDTO.matches.size());
		assertEquals(SRs.id(sr3.getReference()), SRs.id(extendedReferenceDTO.serviceTracker.getServiceReference()));
		assertEquals(foo, extendedReferenceDTO.serviceTracker.getService());

		assertTrue(p1.timeout(200).getValue());
	}

	@Test
	public void test_D_R_M_U_Service() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(
			CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE,
			Arrays.asList(
				"org.apache.aries.cdi.container.test.beans.Reference_D_R_M_U_Service"
			)
		);

		when(
			bundle.adapt(
				BundleWiring.class).getRequiredWires(
					ExtenderNamespace.EXTENDER_NAMESPACE).get(
						0).getRequirement().getAttributes()
		).thenReturn(attributes);

		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = TestUtil.mockCaSt(bundle);

		MockConfiguration mockConfiguration = new MockConfiguration("foo.config", null);
		mockConfiguration.update(Maps.dict("fiz", "buz"));
		TestUtil.configurations.add(mockConfiguration);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, caTracker, new Logs.Builder(bundle.getBundleContext()).build());

		ContainerComponent containerComponent = new ContainerComponent.Builder(containerState,
			new ContainerActivator.Builder(containerState, null)
		).template(
			containerState.containerDTO().template.components.get(0)
		).build();

		Promise<Boolean> p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.OPEN && op.type == Op.Type.REFERENCES;
			}
		);

		containerComponent.open();

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);
		assertEquals(1, containerDTO.changeCount);
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		assertNotNull(p0.timeout(200).getValue());

		ComponentInstanceDTO containerComponentInstance = containerDTO.components.get(0).instances.get(0);

		List<ExtendedReferenceDTO> unresolvedReferences = containerComponentInstance.references.stream().map(
			r -> (ExtendedReferenceDTO)r
		).filter(
			r -> r.matches.size() < r.minimumCardinality
		).collect(Collectors.toList());

		assertEquals(1, unresolvedReferences.size());

		ExtendedReferenceDTO extendedReferenceDTO = unresolvedReferences.get(0);

		assertTrue(extendedReferenceDTO.matches.isEmpty());
		assertEquals(1, extendedReferenceDTO.minimumCardinality);
		assertNotNull(extendedReferenceDTO.serviceTracker);
		assertEquals("(objectClass=org.apache.aries.cdi.container.test.beans.Foo)", extendedReferenceDTO.targetFilter);
		assertNotNull(extendedReferenceDTO.template);
		assertEquals(MaximumCardinality.ONE, extendedReferenceDTO.template.maximumCardinality);
		assertEquals(1, extendedReferenceDTO.template.minimumCardinality);
		assertEquals("org.apache.aries.cdi.container.test.beans.Reference_D_R_M_U_Service.foo", extendedReferenceDTO.template.name);
		assertEquals(ReferencePolicy.DYNAMIC, extendedReferenceDTO.template.policy);
		assertEquals(ReferencePolicyOption.RELUCTANT, extendedReferenceDTO.template.policyOption);
		assertEquals(Foo.class.getName(), extendedReferenceDTO.template.serviceType);
		assertEquals("", extendedReferenceDTO.template.targetFilter);

		// first test publishing a service targeting one of the optional references

		BundleDTO serviceBundleDTO = new BundleDTO();

		Bundle serviceBundle = TestUtil.mockBundle(serviceBundleDTO, b -> {});

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.OPEN && op.type == Op.Type.CONTAINER_ACTIVATOR;
			}
		);

		ServiceRegistration<Foo> sr1 = serviceBundle.getBundleContext().registerService(
			Foo.class, new Foo() {}, Maps.dict("sr1", "sr1"));

		assertTrue(p0.timeout(200).getValue());

		assertEquals(1, extendedReferenceDTO.matches.size());

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.CLOSE && op.type == Op.Type.CONTAINER_INSTANCE;
			}
		);

		serviceBundle.getBundleContext().registerService(
			Foo.class, new Foo() {}, Maps.dict("sr2", "sr2"));

		p0.timeout(200).getFailure();

		assertEquals(2, extendedReferenceDTO.matches.size());

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.CLOSE && op.type == Op.Type.CONTAINER_INSTANCE;
			}
		);

		Foo foo = new Foo() {};

		ServiceRegistration<Foo> sr3 = serviceBundle.getBundleContext().registerService(
			Foo.class, foo, Maps.dict("sr3", "sr3", Constants.SERVICE_RANKING, 100));

		p0.timeout(200).getFailure();

		assertEquals(3, extendedReferenceDTO.matches.size());

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.CLOSE && op.type == Op.Type.CONTAINER_INSTANCE;
			}
		);
		Promise<Boolean> p1 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.OPEN && op.type == Op.Type.CONTAINER_INSTANCE;
			}
		);

		sr1.unregister();

		p0.timeout(200).getFailure();

		assertEquals(2, extendedReferenceDTO.matches.size());
		assertEquals(SRs.id(sr3.getReference()), SRs.id(extendedReferenceDTO.serviceTracker.getServiceReference()));
		assertEquals(foo, extendedReferenceDTO.serviceTracker.getService());

		p1.timeout(200).getFailure();
	}
}
