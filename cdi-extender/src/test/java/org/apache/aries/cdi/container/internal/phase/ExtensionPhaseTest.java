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

import static org.apache.aries.cdi.container.internal.util.Reflection.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.container.CDIBundle;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.ExtensionPhase;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.apache.aries.cdi.container.test.MockServiceRegistration;
import org.apache.aries.cdi.container.test.TestUtil;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Namespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.ExtensionDTO;
import org.osgi.util.promise.Deferred;

public class ExtensionPhaseTest extends BaseCDIBundleTest {

	@Test
	public void extensions_tracking() throws Exception {
		BundleWire wire0 = mock(BundleWire.class);
		BundleRequirement req0 = mock(BundleRequirement.class);
		BundleRevision rev0 = mock(BundleRevision.class);

		when(
			bundle.adapt(
				BundleWiring.class).getRequiredWires(CDIConstants.CDI_EXTENSION_PROPERTY)
		).thenReturn(Arrays.asList(wire0));
		when(
			wire0.getRequirement()
		).thenReturn(req0);
		when(
			wire0.getProvider()
		).thenReturn(rev0);
		when(
			rev0.getBundle()
		).thenReturn(bundle);
		when(
			req0.getDirectives()
		).thenReturn(Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(foo=name)"));

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, null, new Logs.Builder(bundle.getBundleContext()).build());

		CDIBundle cdiBundle = new CDIBundle(
			ccr, containerState,
				new ExtensionPhase(containerState, null));

		cdiBundle.start();

		Collection<ContainerDTO> containerDTOs = ccr.getContainerDTOs(bundle);
		assertFalse(containerDTOs.isEmpty());
		ContainerDTO containerDTO = containerDTOs.iterator().next();
		assertNotNull(containerDTO);

		assertNotNull(containerDTO.bundle);
		assertEquals(1, containerDTO.bundle.id);
		assertEquals(24l, containerDTO.bundle.lastModified);
		assertEquals(Bundle.ACTIVE, containerDTO.bundle.state);
		assertEquals("foo", containerDTO.bundle.symbolicName);
		assertEquals("1.0.0", containerDTO.bundle.version);

		assertEquals(1, containerDTO.changeCount);

		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertTrue(containerDTO.extensions + "", containerDTO.extensions.isEmpty());

		assertNotNull(containerDTO.template);
		assertEquals(1, containerDTO.template.extensions.size());
		assertEquals("(&(foo=name)(service.bundleid=1))", containerDTO.template.extensions.get(0).serviceFilter);

		final MockServiceRegistration<Extension> regA = cast(bundle.getBundleContext().registerService(
			Extension.class, new Extension(){}, Maps.dict("foo", "name")));

		Deferred<ServiceListener> slD = testPromiseFactory.deferred();

		do {
			TestUtil.serviceListeners.stream().filter(
				en -> en.getValue().matches(
					Maps.of(Constants.OBJECTCLASS, Extension.class.getName(),
					"foo", "name", Constants.SERVICE_BUNDLEID, bundle.getBundleId()))
			).map(
				en -> en.getKey()
			).findFirst().ifPresent(
				sl -> slD.resolve(sl)
			);

			Thread.sleep(10);
		} while(!slD.getPromise().timeout(500).isDone());

		slD.getPromise().timeout(500).thenAccept(
			sl -> {
				assertEquals(2, containerState.containerDTO().changeCount);
				assertEquals(1, containerState.containerDTO().extensions.size());
				long id = (long)regA.getReference().getProperty(Constants.SERVICE_ID);
				ExtensionDTO e = containerState.containerDTO().extensions.get(0);
				assertEquals(id, e.service.id);

				final MockServiceRegistration<Extension> regB = cast(bundle.getBundleContext().registerService(
					Extension.class, new Extension(){}, Maps.dict("foo", "name", Constants.SERVICE_RANKING, 10)));

				assertEquals(3, containerState.containerDTO().changeCount);
				assertEquals(1, containerState.containerDTO().extensions.size());
				assertEquals(regB.getReference().getProperty(Constants.SERVICE_ID), containerState.containerDTO().extensions.get(0).service.id);

				sl.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, regB.getReference()));

				assertEquals(4, containerState.containerDTO().changeCount);
				assertEquals(1, containerState.containerDTO().extensions.size());
				assertEquals(regA.getReference().getProperty(Constants.SERVICE_ID), containerState.containerDTO().extensions.get(0).service.id);

				sl.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, regA.getReference()));

				assertEquals(0, containerState.containerDTO().extensions.size());
			}
		).getValue();

		cdiBundle.destroy();
	}

}
