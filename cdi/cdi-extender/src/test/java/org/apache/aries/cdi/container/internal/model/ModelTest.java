package org.apache.aries.cdi.container.internal.model;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.service.cdi.CdiExtenderConstants;
import org.osgi.service.cdi.annotations.PropertyType;
import org.osgi.service.cdi.annotations.ServiceProperty;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.StandardConverter;

import junit.framework.TestCase;

public class ModelTest extends TestCase {

	public void testModelWithBeansOnly() throws Exception {
		AbstractModelBuilder builder = getBuilder("OSGI-INF/cdi/beans-only.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = beansModel.getBeanClassNames();
		assertEquals(2, beanClassNames.size());
		assertEquals("com.foo.FooImpl", beanClassNames.iterator().next());
	}

	public void testModelWithConfiguration() throws Exception {
		AbstractModelBuilder builder = getBuilder("OSGI-INF/cdi/beans-configuration.xml");
		BeansModel beansModel = builder.build();
		assertNotNull(beansModel);

		Collection<String> beanClassNames = beansModel.getBeanClassNames();
		assertEquals(2, beanClassNames.size());
		assertEquals("com.foo.FooImpl", beanClassNames.iterator().next());

		Collection<ConfigurationModel> configurationModels = beansModel.getConfigurationModels();
		assertEquals(2, configurationModels.size());
		ConfigurationModel configurationModel = configurationModels.iterator().next();
		assertEquals("com.foo.FooImpl", configurationModel.getPid());
	}

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
		ServiceProperty[] serviceProperties = serviceModel.getProperties();
		assertEquals(18, serviceProperties.length);

		assertEquals("foo", serviceProperties[0].key());
		assertEquals(PropertyType.String, serviceProperties[0].type());
		assertEquals("fum", getValue(serviceProperties[0]));

		assertEquals("foo2", serviceProperties[1].key());
		assertEquals(PropertyType.Long, serviceProperties[1].type());
		assertEquals(345987643L, getValue(serviceProperties[1]));

		assertEquals("foo3", serviceProperties[2].key());
		assertEquals(PropertyType.Double, serviceProperties[2].type());
		assertEquals(3459.87643D, getValue(serviceProperties[2]));

		assertEquals("foo4", serviceProperties[3].key());
		assertEquals(PropertyType.Float, serviceProperties[3].type());
		assertEquals(3459.87F, getValue(serviceProperties[3]));

		assertEquals("foo5", serviceProperties[4].key());
		assertEquals(PropertyType.Integer, serviceProperties[4].type());
		assertEquals(724559, getValue(serviceProperties[4]));

		assertEquals("foo6", serviceProperties[5].key());
		assertEquals(PropertyType.Byte, serviceProperties[5].type());
		assertEquals(new Byte("127"), getValue(serviceProperties[5]));

		assertEquals("foo7", serviceProperties[6].key());
		assertEquals(PropertyType.Character, serviceProperties[6].type());
		assertEquals(new Character('X'), getValue(serviceProperties[6]));

		assertEquals("foo8", serviceProperties[7].key());
		assertEquals(PropertyType.Boolean, serviceProperties[7].type());
		assertEquals(true, getValue(serviceProperties[7]));

		assertEquals("foo9", serviceProperties[8].key());
		assertEquals(PropertyType.Short, serviceProperties[8].type());
		assertEquals(new Short("32767"), getValue(serviceProperties[8]));

		assertEquals("foo11", serviceProperties[9].key());
		assertEquals(PropertyType.String_Array, serviceProperties[9].type());
		assertTrue(Arrays.equals(new String[] {"frog", "drum"}, (String[])getValue(serviceProperties[9])));

		assertEquals("foo12", serviceProperties[10].key());
		assertEquals(PropertyType.Long_Array, serviceProperties[10].type());
		assertTrue(Arrays.equals(new Long[] {345987643L, 34L}, (Long[])getValue(serviceProperties[10])));

		assertEquals("foo13", serviceProperties[11].key());
		assertEquals(PropertyType.Double_Array, serviceProperties[11].type());
		assertTrue(Arrays.equals(new Double[] {3459.87643D, 34.3456D}, (Double[])getValue(serviceProperties[11])));

		assertEquals("foo14", serviceProperties[12].key());
		assertEquals(PropertyType.Float_Array, serviceProperties[12].type());
		assertTrue(Arrays.equals(new Float[] {3459.87F, 35.23F}, (Float[])getValue(serviceProperties[12])));

		assertEquals("foo15", serviceProperties[13].key());
		assertEquals(PropertyType.Integer_Array, serviceProperties[13].type());
		assertTrue(Arrays.equals(new Integer[] {724559, 345}, (Integer[])getValue(serviceProperties[13])));

		assertEquals("foo16", serviceProperties[14].key());
		assertEquals(PropertyType.Byte_Array, serviceProperties[14].type());
		assertTrue(Arrays.equals(new Byte[] {127, 23}, (Byte[])getValue(serviceProperties[14])));

		assertEquals("foo17", serviceProperties[15].key());
		assertEquals(PropertyType.Character_Array, serviceProperties[15].type());
		assertTrue(Arrays.equals(new Character[] {'X', 't'}, (Character[])getValue(serviceProperties[15])));

		assertEquals("foo18", serviceProperties[16].key());
		assertEquals(PropertyType.Boolean_Array, serviceProperties[16].type());
		assertTrue(Arrays.equals(new Boolean[] {true, false}, (Boolean[])getValue(serviceProperties[16])));

		assertEquals("foo19", serviceProperties[17].key());
		assertEquals(PropertyType.Short_Array, serviceProperties[17].type());
		assertTrue(Arrays.equals(new Short[] {32767, 2345}, (Short[])getValue(serviceProperties[17])));
	}

	AbstractModelBuilder getBuilder(final String osgiBeansFile) {
		return new AbstractModelBuilder() {

			@Override
			Collection<String> getResources(String descriptorString) {
				return null;
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
				return Collections.singletonMap(
					CdiExtenderConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE, osgiBeansFile);
			}
		};
	}

	Object getValue(ServiceProperty serviceProperty) {
		Type type = serviceProperty.type().getType();
		String[] value = serviceProperty.value();
		return _converter.convert(value).to(type);
	}

	private static final Converter _converter = new StandardConverter();

}
