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
import java.util.Map;

import org.apache.aries.cdi.container.internal.container.CDIBundle;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;

public class CDIBundlePhaseTest extends BaseCDIBundleTest {

	@Test
	public void initial() throws Exception {
		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, null, new Logs.Builder(bundle.getBundleContext()).build());

		CDIBundle cdiBundle = new CDIBundle(ccr, containerState, null);

		cdiBundle.start();

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
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertTrue(containerDTO.extensions + "", containerDTO.extensions.isEmpty());

		assertNotNull(containerDTO.template);
		assertEquals(2, containerDTO.template.components.size());
		assertEquals(0, containerDTO.template.extensions.size());
		assertEquals("foo", containerDTO.template.id);

		cdiBundle.destroy();
	}

	@Test
	public void extensions_simple() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(CDIConstants.REQUIREMENT_EXTENSIONS_ATTRIBUTE, Arrays.asList("(foo=name)", "(fum=bar)"));
		attributes.put(CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE,
			Arrays.asList(
				"org.apache.aries.cdi.container.test.beans.BarAnnotated",
				"org.apache.aries.cdi.container.test.beans.FooAnnotated",
				"org.apache.aries.cdi.container.test.beans.FooWithReferenceAndConfig"
			)
		);

		when(
			bundle.adapt(
				BundleWiring.class).getRequiredWires(
					ExtenderNamespace.EXTENDER_NAMESPACE).get(
						0).getRequirement().getAttributes()
		).thenReturn(attributes);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, null, new Logs.Builder(bundle.getBundleContext()).build());

		CDIBundle cdiBundle = new CDIBundle(ccr, containerState, null);

		cdiBundle.start();

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
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertTrue(containerDTO.extensions + "", containerDTO.extensions.isEmpty());

		assertNotNull(containerDTO.template);
		assertEquals(2, containerDTO.template.components.size());
		assertEquals(2, containerDTO.template.extensions.size());
		assertEquals("(foo=name)", containerDTO.template.extensions.get(0).serviceFilter);
		assertEquals("(fum=bar)", containerDTO.template.extensions.get(1).serviceFilter);
		assertEquals("foo", containerDTO.template.id);

		cdiBundle.destroy();
	}

	@Test
	public void extensions_invalidsyntax() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(CDIConstants.REQUIREMENT_EXTENSIONS_ATTRIBUTE, Arrays.asList("(foo=name)", "fum=bar)"));

		when(
			bundle.adapt(
				BundleWiring.class).getRequiredWires(
					ExtenderNamespace.EXTENDER_NAMESPACE).get(
						0).getRequirement().getAttributes()
		).thenReturn(attributes);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, null, new Logs.Builder(bundle.getBundleContext()).build());

		CDIBundle cdiBundle = new CDIBundle(ccr, containerState, null);

		cdiBundle.start();

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
		assertFalse(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertEquals(1, containerDTO.errors.size());
		assertFalse(containerDTO.template.extensions.isEmpty());
		assertEquals("(foo=name)", containerDTO.template.extensions.get(0).serviceFilter);

		cdiBundle.destroy();
	}

}
