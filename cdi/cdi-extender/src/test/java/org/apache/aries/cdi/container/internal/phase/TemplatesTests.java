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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.apache.aries.cdi.container.test.TestUtil;
import org.apache.aries.cdi.container.test.beans.Bar;
import org.apache.aries.cdi.container.test.beans.Baz;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.ReferencePolicy;
import org.osgi.service.cdi.ReferencePolicyOption;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ActivationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class TemplatesTests extends BaseCDIBundleTest {

	@Test
	public void components_simple() throws Exception {
		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, null, new Logs.Builder(bundle.getBundleContext()).build());

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);

		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());

		assertNotNull(containerDTO.template);
		List<ComponentTemplateDTO> components = TestUtil.sort(
			containerDTO.template.components, (a, b) -> a.name.compareTo(b.name));
		assertEquals(2, components.size());

		{
			ComponentTemplateDTO template = components.get(0);
			assertEquals(1, template.activations.size());

			{
				ActivationTemplateDTO at = template.activations.get(0);
				assertEquals(Maps.of("jaxrs.resource", true), at.properties);
				assertEquals(ServiceScope.SINGLETON, at.scope);
				assertEquals(Arrays.asList(Baz.class.getName()), at.serviceClasses);
			}

			assertEquals(1, template.beans.size());
			assertEquals(2, template.configurations.size());
			assertEquals("foo", template.name);
			assertEquals(Maps.of(), template.properties);
			assertEquals(6, template.references.size());
			assertEquals(ComponentType.CONTAINER, template.type);
		}

		{
			ComponentTemplateDTO template = components.get(1);
			assertEquals(1, template.activations.size());

			{
				ActivationTemplateDTO at = template.activations.get(0);
				assertEquals(Maps.of(), at.properties);
				assertEquals(ServiceScope.SINGLETON, at.scope);
				assertEquals(Arrays.asList("org.apache.aries.cdi.container.test.beans.Foo"), at.serviceClasses);
			}

			assertEquals(2, template.beans.size());
			assertEquals(2, template.configurations.size());
			assertEquals("foo.annotated", template.name);
			assertEquals(Maps.of("service.ranking", 12), template.properties);
			assertEquals(3, template.references.size());
			assertEquals(ComponentType.SINGLE, template.type);
		}
	}

	@Test
	public void components_multiple() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(
			CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE,
			Arrays.asList(
				"org.apache.aries.cdi.container.test.beans.BarAnnotated",
				"org.apache.aries.cdi.container.test.beans.BarProducer",
				"org.apache.aries.cdi.container.test.beans.FooAnnotated",
				"org.apache.aries.cdi.container.test.beans.FooWithReferenceAndConfig",
				"org.apache.aries.cdi.container.test.beans.ObserverFoo",
				"org.apache.aries.cdi.container.test.beans.BarService"
			)
		);

		when(
			bundle.adapt(
				BundleWiring.class).getRequiredWires(
					ExtenderNamespace.EXTENDER_NAMESPACE).get(
						0).getRequirement().getAttributes()
		).thenReturn(attributes);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, null, new Logs.Builder(bundle.getBundleContext()).build());

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);

		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());

		assertNotNull(containerDTO.template);
		List<ComponentTemplateDTO> components = TestUtil.sort(
			containerDTO.template.components, (a, b) -> a.name.compareTo(b.name));
		assertEquals(3, components.size());

		{
			ComponentTemplateDTO ct = components.get(0);
			assertEquals(1, ct.activations.size());

			{
				ActivationTemplateDTO at = ct.activations.get(0);
				assertEquals(Maps.of(), at.properties);
				assertEquals(ServiceScope.SINGLETON, at.scope);
				assertEquals(Arrays.asList("org.apache.aries.cdi.container.test.beans.Bar"), at.serviceClasses);
			}

			assertEquals(1, ct.beans.size());
			assertEquals(1, ct.configurations.size());
			assertEquals("barService", ct.name);
			assertEquals(Maps.of(), ct.properties);
			assertEquals(0, ct.references.size());
			assertEquals(ComponentType.FACTORY, ct.type);
		}

		{
			ComponentTemplateDTO ct = components.get(1);
			assertEquals(3, ct.activations.size());
			assertEquals(3, ct.beans.size());
			assertEquals(2, ct.configurations.size());
			assertEquals("foo", ct.name);
			assertEquals(Maps.of(), ct.properties);
			assertEquals(8, ct.references.size());
			assertEquals(ComponentType.CONTAINER, ct.type);
		}

		{
			ComponentTemplateDTO ct = components.get(2);
			assertEquals(1, ct.activations.size());

			{
				ActivationTemplateDTO at = ct.activations.get(0);
				assertEquals(Maps.of(), at.properties);
				assertEquals(ServiceScope.SINGLETON, at.scope);
				assertEquals(Arrays.asList("org.apache.aries.cdi.container.test.beans.Foo"), at.serviceClasses);
			}

			assertEquals(2, ct.beans.size());
			assertEquals(2, ct.configurations.size());
			assertEquals("foo.annotated", ct.name);
			assertEquals(Maps.of("service.ranking", 12), ct.properties);
			assertEquals(3, ct.references.size());
			assertEquals(ComponentType.SINGLE, ct.type);
		}
	}

	@Test
	public void components_verifyContainerComponent() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(
			CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE,
			Arrays.asList(
				"org.apache.aries.cdi.container.test.beans.BarAnnotated",
				"org.apache.aries.cdi.container.test.beans.BarProducer",
				"org.apache.aries.cdi.container.test.beans.FooAnnotated",
				"org.apache.aries.cdi.container.test.beans.FooWithReferenceAndConfig",
				"org.apache.aries.cdi.container.test.beans.ObserverFoo",
				"org.apache.aries.cdi.container.test.beans.BarService"
			)
		);

		when(
			bundle.adapt(
				BundleWiring.class).getRequiredWires(
					ExtenderNamespace.EXTENDER_NAMESPACE).get(
						0).getRequirement().getAttributes()
		).thenReturn(attributes);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, null, new Logs.Builder(bundle.getBundleContext()).build());

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);

		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		List<ComponentTemplateDTO> components = TestUtil.sort(
			containerDTO.template.components, (a, b) -> a.name.compareTo(b.name));
		assertEquals(3, components.size());

		{ // component "barService"
			ComponentTemplateDTO ct = components.get(0);
			assertEquals(1, ct.activations.size());

			{
				ActivationTemplateDTO at = ct.activations.get(0);
				assertEquals(Maps.of(), at.properties);
				assertEquals(ServiceScope.SINGLETON, at.scope);
				assertEquals(Arrays.asList("org.apache.aries.cdi.container.test.beans.Bar"), at.serviceClasses);
			}

			assertEquals(1, ct.beans.size());
			assertEquals("org.apache.aries.cdi.container.test.beans.BarService", ct.beans.get(0));
			assertEquals(1, ct.configurations.size());
			assertEquals("barService", ct.name);
			assertEquals(Maps.of(), ct.properties);
			assertEquals(0, ct.references.size());
			assertEquals(ComponentType.FACTORY, ct.type);

			{ // configuration "barService"
				ConfigurationTemplateDTO configurationTemplateDTO = ct.configurations.get(0);
				assertEquals(true, configurationTemplateDTO.componentConfiguration);
				assertEquals(MaximumCardinality.MANY, configurationTemplateDTO.maximumCardinality);
				assertEquals("barService", configurationTemplateDTO.pid);
				assertEquals(ConfigurationPolicy.REQUIRED, configurationTemplateDTO.policy);
			}
		}

		{ // component "foo"
			ComponentTemplateDTO ct = components.get(1);

			assertEquals(3, ct.activations.size());

			{
				ActivationTemplateDTO at = ct.activations.get(0);
				assertEquals(Maps.of("jaxrs.resource", true), at.properties);
				assertEquals(ServiceScope.SINGLETON, at.scope);
				assertEquals(Arrays.asList(Baz.class.getName()), at.serviceClasses);
			}

			{
				ActivationTemplateDTO at = ct.activations.get(1);
				assertEquals(Maps.of(Constants.SERVICE_RANKING, 100), at.properties);
				assertEquals(ServiceScope.BUNDLE, at.scope);
				assertEquals(Arrays.asList(Integer.class.getName()), at.serviceClasses);
			}

			{
				ActivationTemplateDTO at = ct.activations.get(2);
				assertEquals(Maps.of(), at.properties);
				assertEquals(ServiceScope.SINGLETON, at.scope);
				assertEquals(Arrays.asList(Bar.class.getName()), at.serviceClasses);
			}

			assertEquals(3, ct.beans.size());

			List<String> beans = TestUtil.sort(ct.beans, (a, b) -> a.compareTo(b));
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated", beans.get(0));
			assertEquals("org.apache.aries.cdi.container.test.beans.BarProducer", beans.get(1));
			assertEquals("org.apache.aries.cdi.container.test.beans.ObserverFoo", beans.get(2));

			assertEquals(2, ct.configurations.size());
			assertEquals("foo", ct.name);
			assertEquals(Maps.of(), ct.properties);
			assertEquals(8, ct.references.size());
			assertEquals(ComponentType.CONTAINER, ct.type);

			{ // configuration "osgi.cdi.foo"
				ConfigurationTemplateDTO configurationTemplateDTO = ct.configurations.get(0);
				assertEquals(true, configurationTemplateDTO.componentConfiguration);
				assertEquals(MaximumCardinality.ONE, configurationTemplateDTO.maximumCardinality);
				assertEquals("osgi.cdi.foo", configurationTemplateDTO.pid);
				assertEquals(ConfigurationPolicy.OPTIONAL, configurationTemplateDTO.policy);
			}

			{ // "foo.config
				ConfigurationTemplateDTO configurationTemplateDTO = ct.configurations.get(1);
				assertEquals(false, configurationTemplateDTO.componentConfiguration);
				assertEquals(MaximumCardinality.ONE, configurationTemplateDTO.maximumCardinality);
				assertEquals("foo.config", configurationTemplateDTO.pid);
				assertEquals(ConfigurationPolicy.REQUIRED, configurationTemplateDTO.policy);
			}

			List<ReferenceTemplateDTO> references = TestUtil.sort(ct.references, (a, b) -> a.name.compareTo(b.name));

			{
				ReferenceTemplateDTO ref = references.get(0);
				assertEquals(MaximumCardinality.MANY, ref.maximumCardinality);
				assertEquals(0, ref.minimumCardinality);
				assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.dynamicFoos", ref.name);
				assertEquals(ReferencePolicy.DYNAMIC, ref.policy);
				assertEquals(ReferencePolicyOption.RELUCTANT, ref.policyOption);
				assertEquals("org.apache.aries.cdi.container.test.beans.Foo", ref.serviceType);
				assertEquals("", ref.targetFilter);
			}

			{
				ReferenceTemplateDTO ref = references.get(1);
				assertEquals(MaximumCardinality.ONE, ref.maximumCardinality);
				assertEquals(1, ref.minimumCardinality);
				assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.foo", ref.name);
				assertEquals(ReferencePolicy.STATIC, ref.policy);
				assertEquals(ReferencePolicyOption.GREEDY, ref.policyOption);
				assertEquals("org.apache.aries.cdi.container.test.beans.Foo", ref.serviceType);
				assertEquals("", ref.targetFilter);
			}

			{
				ReferenceTemplateDTO ref = references.get(2);
				assertEquals(MaximumCardinality.ONE, ref.maximumCardinality);
				assertEquals(0, ref.minimumCardinality);
				assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.fooOptional", ref.name);
				assertEquals(ReferencePolicy.STATIC, ref.policy);
				assertEquals(ReferencePolicyOption.RELUCTANT, ref.policyOption);
				assertEquals("org.apache.aries.cdi.container.test.beans.Foo", ref.serviceType);
				assertEquals("", ref.targetFilter);
			}

			{
				ReferenceTemplateDTO ref = references.get(3);
				assertEquals(MaximumCardinality.MANY, ref.maximumCardinality);
				assertEquals(0, ref.minimumCardinality);
				assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.propertiesFoos", ref.name);
				assertEquals(ReferencePolicy.STATIC, ref.policy);
				assertEquals(ReferencePolicyOption.RELUCTANT, ref.policyOption);
				assertEquals("org.apache.aries.cdi.container.test.beans.Foo", ref.serviceType);
				assertEquals("", ref.targetFilter);
			}

			{
				ReferenceTemplateDTO ref = references.get(4);
				assertEquals(MaximumCardinality.MANY, ref.maximumCardinality);
				assertEquals(0, ref.minimumCardinality);
				assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.serviceReferencesFoos", ref.name);
				assertEquals(ReferencePolicy.STATIC, ref.policy);
				assertEquals(ReferencePolicyOption.RELUCTANT, ref.policyOption);
				assertEquals("org.apache.aries.cdi.container.test.beans.Foo", ref.serviceType);
				assertEquals("(service.scope=prototype)", ref.targetFilter);
			}

			{
				ReferenceTemplateDTO ref = references.get(5);
				assertEquals(MaximumCardinality.MANY, ref.maximumCardinality);
				assertEquals(0, ref.minimumCardinality);
				assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.tupleIntegers", ref.name);
				assertEquals(ReferencePolicy.STATIC, ref.policy);
				assertEquals(ReferencePolicyOption.RELUCTANT, ref.policyOption);
				assertEquals("java.lang.Integer", ref.serviceType);
				assertEquals("", ref.targetFilter);
			}

			{
				ReferenceTemplateDTO ref = references.get(6);
				assertEquals(MaximumCardinality.ONE, ref.maximumCardinality);
				assertEquals(1, ref.minimumCardinality);
				assertEquals("org.apache.aries.cdi.container.test.beans.BarProducer.getBar0", ref.name);
				assertEquals(ReferencePolicy.STATIC, ref.policy);
				assertEquals(ReferencePolicyOption.RELUCTANT, ref.policyOption);
				assertEquals("org.apache.aries.cdi.container.test.beans.Bar", ref.serviceType);
				assertEquals("", ref.targetFilter);
			}

			{
				ReferenceTemplateDTO ref = references.get(7);
				assertEquals(MaximumCardinality.MANY, ref.maximumCardinality);
				assertEquals(0, ref.minimumCardinality);
				assertEquals("org.apache.aries.cdi.container.test.beans.ObserverFoo.foos0", ref.name);
				assertEquals(ReferencePolicy.DYNAMIC, ref.policy);
				assertEquals(ReferencePolicyOption.GREEDY, ref.policyOption);
				assertEquals("org.apache.aries.cdi.container.test.beans.Foo", ref.serviceType);
				assertEquals("", ref.targetFilter);
			}
		}

		{ // component "foo.annotated"
			ComponentTemplateDTO ct = components.get(2);
			assertEquals(1, ct.activations.size());
			assertEquals(2, ct.beans.size());
			assertEquals("org.apache.aries.cdi.container.test.beans.FooAnnotated", ct.beans.get(0));
			assertEquals(2, ct.configurations.size());
			assertEquals("foo.annotated", ct.name);
			assertEquals(Maps.of("service.ranking", 12), ct.properties);
			assertEquals(3, ct.references.size());
			assertEquals(ComponentType.SINGLE, ct.type);

			{ // configuration "foo.annotated"
				ConfigurationTemplateDTO configurationTemplateDTO = ct.configurations.get(0);
				assertEquals(true, configurationTemplateDTO.componentConfiguration);
				assertEquals(MaximumCardinality.ONE, configurationTemplateDTO.maximumCardinality);
				assertEquals("foo.annotated", configurationTemplateDTO.pid);
				assertEquals(ConfigurationPolicy.OPTIONAL, configurationTemplateDTO.policy);
			}
		}
	}

	@Test
	public void descriptor_missingbeanclass() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(
			CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE,
			Arrays.asList(
				"org.apache.aries.cdi.container.test.beans.BarAnnotated",
				"org.apache.aries.cdi.container.test.beans.FooAnnotated",
				"org.apache.aries.cdi.container.test.beans.Missing"
			)
		);

		when(
			bundle.adapt(
				BundleWiring.class).getRequiredWires(
					ExtenderNamespace.EXTENDER_NAMESPACE).get(
						0).getRequirement().getAttributes()
		).thenReturn(attributes);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, null, new Logs.Builder(bundle.getBundleContext()).build());

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);

		assertNotNull(containerDTO.bundle);
		assertEquals(1, containerDTO.bundle.id);
		assertEquals(24l, containerDTO.bundle.lastModified);
		assertEquals(Bundle.ACTIVE, containerDTO.bundle.state);
		assertEquals("foo", containerDTO.bundle.symbolicName);
		assertEquals("1.0.0", containerDTO.bundle.version);

		assertEquals(1, containerDTO.changeCount);

		assertTrue(containerDTO.components + "", containerDTO.components.isEmpty());
		assertFalse(containerDTO.errors.isEmpty());
		String[] linesOfError = containerDTO.errors.get(0).split("\r\n|\r|\n", 4);
		assertTrue(linesOfError[0], linesOfError[0].contains("java.lang.ClassNotFoundException: org.apache.aries.cdi.container.test.beans.Missing"));
	}

}
