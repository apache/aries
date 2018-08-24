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

package org.apache.aries.cdi.test.cases;

import static org.junit.Assert.*;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;

public class CdiContainerTests extends AbstractTestCase {

	@Test
	public void testGetBeanFromCdiContainerService() throws Exception {
		BeanManager beanManager = getBeanManager(cdiBundle);

		assertNotNull(beanManager);
		assertBeanExists(Pojo.class, beanManager);
	}

	@Test
	@Ignore("Due to a Service Loader Mediator incompatibility in the official CDI 2.0 API")
	public void testGetBeanManagerFromCDI() throws Exception {
		BeanManager beanManager = with(
			cdiBundle.adapt(BundleWiring.class).getClassLoader(),
			() ->
				CDI.current().getBeanManager()
		);

		assertNotNull(beanManager);
		assertBeanExists(Pojo.class, beanManager);
	}

	@Test
	public void testContainerComponentSingleton() throws Exception {
		while (getContainerDTO(cdiRuntime, cdiBundle).components.isEmpty()) {
			Thread.sleep(10);
		}

		ContainerDTO containerDTO = getContainerDTO(cdiRuntime, cdiBundle);
		assertNotNull(containerDTO);

		ComponentDTO containerComponentDTO = containerDTO.components.stream()
				.filter(c -> c.template.type == ComponentType.CONTAINER)
				.findFirst()
				.orElse(null);

		ComponentInstanceDTO componentInstanceDTO = containerComponentDTO.instances.get(0);
		assertNotNull(componentInstanceDTO);

		assertEquals(0, componentInstanceDTO.configurations.size());
		assertNotNull("should have properties", componentInstanceDTO.properties);
	}

}