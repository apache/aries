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

package org.apache.aries.cdi.container.internal.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.apache.aries.cdi.container.test.TestUtil;
import org.junit.Test;

public class ModelTest extends TestUtil {

	@Test
	public void testModelWithSingleBean() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(
			"OSGI-INF/cdi/org.apache.aries.cdi.container.test.beans.Bar.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = sort(beansModel.getBeanClassNames(), (c1, c2) -> c1.compareTo(c2));
		assertEquals(1, beanClassNames.size());
		assertEquals("org.apache.aries.cdi.container.test.beans.Bar", beanClassNames.iterator().next());
	}

	@Test
	public void testModelWithDefaultResources() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(null);
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = sort(beansModel.getBeanClassNames(), (c1, c2) -> c1.compareTo(c2));
		assertEquals(5, beanClassNames.size());
		assertEquals("org.apache.aries.cdi.container.test.beans.Bar", beanClassNames.iterator().next());
	}
}
