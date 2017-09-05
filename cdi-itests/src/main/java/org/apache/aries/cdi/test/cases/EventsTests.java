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

import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.CdiEventObserverQualifier;
import org.osgi.service.cdi.CdiEvent;

public class EventsTests extends AbstractTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		cdiContainer = waitForCdiContainer(cdiBundle.getBundleId());
	}

	public void testEventsGetSent() throws Exception {
		BeanManager beanManager = cdiContainer.getBeanManager();

		assertNotNull(beanManager);

		@SuppressWarnings("serial")
		Set<Bean<?>> beans = beanManager.getBeans(Object.class, new AnnotationLiteral<CdiEventObserverQualifier>() {});
		Bean<?> bean = beanManager.resolve(beans);
		CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
		Object bcb = beanManager.getReference(bean, Object.class, ctx);
		assertNotNull(bcb);
		@SuppressWarnings("unchecked")
		BeanService<List<CdiEvent>> bti = (BeanService<List<CdiEvent>>)bcb;
		List<CdiEvent> list = bti.get();
		assertNotNull(list);
		assertEquals(1, list.size());
	}

}
