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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osgi.service.cdi.CdiConstants;

public class ModelTest {

	@Test
	public void testModelWithBeansOnly() throws Exception {
		AbstractModelBuilder builder = getBuilder("OSGI-INF/cdi/beans-only.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = beansModel.getBeanClassNames();
		assertEquals(2, beanClassNames.size());
		assertEquals("com.foo.FooImpl", beanClassNames.iterator().next());
	}

	@Test
	public void testModelWithConfiguration() throws Exception {
		AbstractModelBuilder builder = getBuilder("OSGI-INF/cdi/beans-configuration.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = beansModel.getBeanClassNames();
		assertEquals(2, beanClassNames.size());
		assertEquals("com.foo.FooImpl", beanClassNames.iterator().next());

		Collection<ConfigurationModel> configurationModels = beansModel.getConfigurationModels();
		assertEquals(3, configurationModels.size());
		Iterator<ConfigurationModel> iterator = configurationModels.iterator();

		ConfigurationModel configurationModel = iterator.next();
		assertArrayEquals(new String[] {"com.foo.FooImpl"}, configurationModel.pids());
		assertEquals("com.foo.Config", configurationModel.beanClass());
		assertEquals(true, configurationModel.required());

		configurationModel = iterator.next();
		assertArrayEquals(new String[] {"com.foo.other", "and.another"}, configurationModel.pids());
		assertEquals("com.foo.Baz", configurationModel.beanClass());
		assertEquals(true, configurationModel.required());

		configurationModel = iterator.next();
		assertArrayEquals(new String[] {"an.optional.configuration"}, configurationModel.pids());
		assertEquals(false, configurationModel.required());
	}

	@Test
	public void testModelWithReferences() throws Exception {
		AbstractModelBuilder builder = getBuilder("OSGI-INF/cdi/beans-references.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = beansModel.getBeanClassNames();
		assertEquals(2, beanClassNames.size());
		assertEquals("com.foo.FooImpl", beanClassNames.iterator().next());

		Collection<ReferenceModel> referenceModels = beansModel.getReferenceModels();
		assertEquals(2, referenceModels.size());
		ReferenceModel referenceModel = referenceModels.iterator().next();
		assertEquals("java.util.concurrent.Callable", referenceModel.getBeanClass());
		assertEquals("(objectClass=java.util.concurrent.Callable)", referenceModel.getTarget());
	}

	@Test
	public void testModelWithServices() throws Exception {
		AbstractModelBuilder builder = getBuilder("OSGI-INF/cdi/beans-services.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = beansModel.getBeanClassNames();
		assertEquals(2, beanClassNames.size());
		assertEquals("com.foo.FooImpl", beanClassNames.iterator().next());

		Collection<ServiceModel> serviceModels = beansModel.getServiceModels();
		assertEquals(1, serviceModels.size());
		ServiceModel serviceModel = serviceModels.iterator().next();
		List<String> provides = serviceModel.getProvides();
		assertEquals(2, provides.size());
		assertEquals("com.foo.Foo", provides.get(0));
		String[] serviceProperties = serviceModel.getProperties();
		assertEquals(33, serviceProperties.length);

		assertEquals("foo:String=fum", serviceProperties[0]);
		assertEquals("foo1:String=baz", serviceProperties[1]);
		assertEquals("foo2:Long=345987643", serviceProperties[2]);
		assertEquals("foo3:Double=3459.87643", serviceProperties[3]);
		assertEquals("foo4:Float=3459.87", serviceProperties[4]);
		assertEquals("foo5:Integer=724559", serviceProperties[5]);
		assertEquals("foo6:Byte=127", serviceProperties[6]);
		assertEquals("foo7:Character=X", serviceProperties[7]);
		assertEquals("foo8:Boolean=true", serviceProperties[8]);
		assertEquals("foo9:Short=32767", serviceProperties[9]);
		assertEquals("foo11:String=frog", serviceProperties[10]);
		assertEquals("foo11:String=drum", serviceProperties[11]);
		assertEquals("foo12:Long=345987643", serviceProperties[12]);
		assertEquals("foo12:Long=34", serviceProperties[13]);
		assertEquals("foo13:Double=3459.87643", serviceProperties[14]);
		assertEquals("foo13:Double=34.3456", serviceProperties[15]);
		assertEquals("foo14:Float=3459.87", serviceProperties[16]);
		assertEquals("foo14:Float=35.23", serviceProperties[17]);
		assertEquals("foo15:Integer=724559", serviceProperties[18]);
		assertEquals("foo15:Integer=345", serviceProperties[19]);
		assertEquals("foo16:Byte=127", serviceProperties[20]);
		assertEquals("foo16:Byte=23", serviceProperties[21]);
		assertEquals("foo17:Character=X", serviceProperties[22]);
		assertEquals("foo17:Character=t", serviceProperties[23]);
		assertEquals("foo18:Boolean=true", serviceProperties[24]);
		assertEquals("foo18:Boolean=false", serviceProperties[25]);
		assertEquals("foo19:Short=32767", serviceProperties[26]);
		assertEquals("foo19:Short=2345", serviceProperties[27]);
		assertEquals("foo20:String=bar", serviceProperties[28]);
		assertEquals("foo21:List<Short>=32767", serviceProperties[29]);
		assertEquals("foo21:List<Short>=2345", serviceProperties[30]);
		assertEquals("foo22:Set<Short>=32767", serviceProperties[31]);
		assertEquals("foo22:Set<Short>=2345", serviceProperties[32]);
	}

	@Test
	public void testModelWithAllDescriptors() throws Exception {
		AbstractModelBuilder builder = getBuilder(null);
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = beansModel.getBeanClassNames();
		assertEquals(2, beanClassNames.size());
		assertEquals("com.foo.FooImpl", beanClassNames.iterator().next());
	}

	AbstractModelBuilder getBuilder(final String osgiBeansFile) {
		return new AbstractModelBuilder() {

			@Override
			List<String> getDefaultResources() {
				return Arrays.asList(
						"OSGI-INF/cdi/beans-configuration.xml",
						"OSGI-INF/cdi/beans-only.xml",
						"OSGI-INF/cdi/beans-references.xml",
						"OSGI-INF/cdi/beans-services.xml"
					);
			}

			@Override
			URL getResource(String resource) {
				return getClassLoader().getResource(resource);
			}

			@Override
			ClassLoader getClassLoader() {
				return getClass().getClassLoader();
			}

			@Override
			Map<String, Object> getAttributes() {
				if (osgiBeansFile == null) {
					return Collections.emptyMap();
				}

				return Collections.singletonMap(
					CdiConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE, Arrays.asList(osgiBeansFile));
			}
		};
	}

}
