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

import static org.apache.aries.cdi.container.test.TestUtil.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.aries.cdi.container.internal.component.ComponentModel;
import org.apache.aries.cdi.container.internal.configuration.ConfigurationModel;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel;
import org.apache.aries.cdi.container.test.beans.BarReference;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.apache.aries.cdi.container.test.beans.FooReference;
import org.junit.Test;
import org.osgi.service.cdi.annotations.ConfigurationPolicy;

public class ModelTest {

	@Test
	public void testModelWithBeansOnly() throws Exception {
		AbstractModelBuilder builder = getModelBuilder("OSGI-INF/cdi/beans-only.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = sort(beansModel.getBeanClassNames(), (c1, c2) -> c1.compareTo(c2));
		assertEquals(2, beanClassNames.size());
		assertEquals("org.apache.aries.cdi.container.test.beans.Bar", beanClassNames.iterator().next());
	}

	@Test
	public void testModelWithConfiguration() throws Exception {
		AbstractModelBuilder builder = getModelBuilder("OSGI-INF/cdi/beans-configuration.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = sort(beansModel.getBeanClassNames());
		assertEquals(2, beanClassNames.size());
		assertEquals("org.apache.aries.cdi.container.test.beans.BarWithConfig", beanClassNames.iterator().next());

		Collection<ComponentModel> componentModels = beansModel.getComponentModels();
		assertEquals(2, componentModels.size());
		Iterator<ComponentModel> components = componentModels.iterator();

		ComponentModel componentModel = components.next();
		Collection<ConfigurationModel> configurations = sort(componentModel.getConfigurations());
		Iterator<ConfigurationModel> confIterator = configurations.iterator();

		ConfigurationModel configurationModel = confIterator.next();
		assertArrayEquals(new String[] {"$"}, configurationModel.getPid());
		assertEquals(ConfigurationPolicy.OPTIONAL, configurationModel.getConfigurationPolicy());
		assertEquals("org.apache.aries.cdi.container.test.beans.Bar", configurationModel.getType().getTypeName());

		configurationModel = confIterator.next();
		assertArrayEquals(new String[] {"$"}, configurationModel.getPid());
		assertEquals(ConfigurationPolicy.REQUIRE, configurationModel.getConfigurationPolicy());
		assertEquals("org.apache.aries.cdi.container.test.beans.Config", configurationModel.getType().getTypeName());

		componentModel = components.next();
		configurations = sort(componentModel.getConfigurations());
		configurationModel = configurations.iterator().next();
		assertArrayEquals(new String[] {"$", "foo.config"}, configurationModel.getPid());
		assertEquals(ConfigurationPolicy.OPTIONAL, configurationModel.getConfigurationPolicy());
		assertEquals("org.apache.aries.cdi.container.test.beans.Config", configurationModel.getType().getTypeName());
	}

	@Test
	public void testModelWithReferences() throws Exception {
		AbstractModelBuilder builder = getModelBuilder("OSGI-INF/cdi/beans-references.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = sort(beansModel.getBeanClassNames());
		assertEquals(2, beanClassNames.size());
		assertEquals("org.apache.aries.cdi.container.test.beans.BarWithReference", beanClassNames.iterator().next());

		Collection<ComponentModel> componentModels = sort(beansModel.getComponentModels());
		assertEquals(2, componentModels.size());
		Iterator<ComponentModel> iterator = componentModels.iterator();

		ComponentModel componentModel = iterator.next();
		ReferenceModel referenceModel = componentModel.getReferences().iterator().next();
		assertEquals(BarReference.class, referenceModel.getBeanClass());

		componentModel = iterator.next();
		referenceModel = componentModel.getReferences().iterator().next();
		assertEquals(FooReference.class, referenceModel.getBeanClass());
	}

	@Test
	public void testModelWithServices() throws Exception {
		AbstractModelBuilder builder = getModelBuilder("OSGI-INF/cdi/beans-services.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = sort(beansModel.getBeanClassNames());
		assertEquals(2, beanClassNames.size());
		assertEquals("org.apache.aries.cdi.container.test.beans.FooService", beanClassNames.iterator().next());

		Collection<ComponentModel> componentModels = sort(beansModel.getComponentModels(), (a, b) -> a.getName().compareTo(b.getName()));
		assertEquals(2, componentModels.size());

		Iterator<ComponentModel> iterator = componentModels.iterator();

		ComponentModel componentModel = iterator.next();
		List<String> provides = componentModel.getProvides();
		assertEquals(0, provides.size());

		componentModel = iterator.next();
		provides = componentModel.getProvides();
		assertEquals(2, provides.size());

		assertEquals(Foo.class.getName(), provides.get(0));
		assertEquals(Cloneable.class.getName(), provides.get(1));
		String[] serviceProperties = componentModel.getProperties();
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
		AbstractModelBuilder builder = getModelBuilder(null);
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = sort(beansModel.getBeanClassNames());
		assertEquals(8, beanClassNames.size());
		assertEquals("org.apache.aries.cdi.container.test.beans.FooService", beanClassNames.iterator().next());
	}

}
