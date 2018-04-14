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
import java.util.HashMap;
import java.util.Map;

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
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.ExtensionDTO;
import org.osgi.util.promise.Deferred;

public class ExtensionPhaseTest extends BaseCDIBundleTest {

	@Test
	public void extensions_tracking() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(CDIConstants.REQUIREMENT_EXTENSIONS_ATTRIBUTE, Arrays.asList("(foo=name)"));

		when(
			bundle.adapt(
				BundleWiring.class).getRequiredWires(
					ExtenderNamespace.EXTENDER_NAMESPACE).get(
						0).getRequirement().getAttributes()
		).thenReturn(attributes);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, null, new Logs.Builder(bundle.getBundleContext()).build());

		CDIBundle cdiBundle = new CDIBundle(
			ccr, containerState,
				new ExtensionPhase(containerState, null));

		cdiBundle.start();

		ContainerDTO containerDTO = ccr.getContainerDTO(bundle);
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
		assertEquals("(foo=name)", containerDTO.template.extensions.get(0).serviceFilter);

		final MockServiceRegistration<Extension> regA = cast(bundle.getBundleContext().registerService(
			Extension.class, new Extension(){}, Maps.dict("foo", "name")));

		Deferred<ServiceListener> slD = testPromiseFactory.deferred();

		do {
			TestUtil.serviceListeners.stream().filter(
				en -> en.getValue().matches(
					Maps.of(Constants.OBJECTCLASS, Extension.class.getName(),
					"foo", "name"))
			).map(
				en -> en.getKey()
			).findFirst().ifPresent(
				sl -> slD.resolve(sl)
			);

			Thread.sleep(10);
		} while(!slD.getPromise().isDone());

		slD.getPromise().thenAccept(
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
