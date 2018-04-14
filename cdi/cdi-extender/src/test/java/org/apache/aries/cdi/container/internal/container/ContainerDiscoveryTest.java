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

package org.apache.aries.cdi.container.internal.container;

public class ContainerDiscoveryTest {

/*	@Test
	public void testBeansOnly() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(Collections.singletonList("OSGI-INF/cdi/beans-only.xml"), null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = new ContainerState(null, null) {

			@Override
			public BeansModel beansModel() {
				return beansModel;
			}

			public <T extends ResourceLoader & ProxyServices> T loader() {
				return null;
			}

		};

		ContainerDiscovery.discover(containerState);

		Collection<ComponentModel> componentModels = sort(
			beansModel.getComponentModels(), (a, b) -> a.getName().compareTo(b.getName()));

		assertEquals(2, componentModels.size());

		Iterator<ComponentModel> iterator = componentModels.iterator();

		assertComponentModel(
			iterator.next(),
			Bar.class,
			0,
			Bar.class.getName(),
			new String[0],
			Collections.emptyList(),
			0,
			ServiceScope.NONE);

		assertComponentModel(
			iterator.next(),
			Foo.class,
			0,
			Foo.class.getName(),
			new String[0],
			Collections.emptyList(),
			0,
			ServiceScope.NONE);
	}

	@Test
	public void testConfiguration() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(Collections.singletonList("OSGI-INF/cdi/beans-configuration.xml"), null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = new ContainerState(null, null) {

			@Override
			public BeansModel beansModel() {
				return beansModel;
			}

			public <T extends ResourceLoader & ProxyServices> T loader() {
				return null;
			}

		};

		ContainerDiscovery.discover(containerState);

		Collection<ComponentModel> componentModels = sort(
			beansModel.getComponentModels(), (a, b) -> a.getName().compareTo(b.getName()));

		assertEquals(2, componentModels.size());

		Iterator<ComponentModel> compItr = componentModels.iterator();

		ComponentModel componentModel = compItr.next();

		assertComponentModel(
			componentModel,
			BarWithConfig.class,
			2,
			BarWithConfig.class.getName(),
			new String[0],
			Collections.emptyList(),
			0,
			ServiceScope.NONE);

		Collection<ConfigurationModel> configurations = sort(componentModel.getConfigurations(), (a, b) -> a.getType().getTypeName().compareTo(b.getType().getTypeName()));

		Iterator<ConfigurationModel> confItr = configurations.iterator();

		assertConfigurationModel(
			confItr.next(),
			new String[] {"$"},
			ConfigurationPolicy.OPTIONAL,
			new TypeReference<Bar>() {}.getType());

		assertConfigurationModel(
			confItr.next(),
			new String[] {"$"},
			ConfigurationPolicy.REQUIRE,
			new TypeReference<Config>() {}.getType());

		componentModel = compItr.next();

		assertComponentModel(
			componentModel,
			FooWithConfig.class,
			1,
			FooWithConfig.class.getName(),
			new String[0],
			Collections.emptyList(),
			0,
			ServiceScope.NONE);

		configurations = sort(componentModel.getConfigurations(), (a, b) -> a.getType().getTypeName().compareTo(b.getType().getTypeName()));

		confItr = configurations.iterator();

		assertConfigurationModel(
			confItr.next(),
			new String[] {"$", "foo.config"},
			ConfigurationPolicy.OPTIONAL,
			new TypeReference<Config>() {}.getType());
	}

	@Test
	public void testReferences() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(Collections.singletonList("OSGI-INF/cdi/beans-references.xml"), null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = new ContainerState(null, null) {

			@Override
			public BeansModel beansModel() {
				return beansModel;
			}

			public <T extends ResourceLoader & ProxyServices> T loader() {
				return null;
			}

		};

		ContainerDiscovery.discover(containerState);

		Collection<ComponentModel> componentModels = sort(
			beansModel.getComponentModels(), (a, b) -> a.getName().compareTo(b.getName()));

		assertEquals(2, componentModels.size());

		Iterator<ComponentModel> compItr = componentModels.iterator();

		ComponentModel componentModel = compItr.next();

		assertComponentModel(
			componentModel,
			BarWithReference.class,
			0,
			BarWithReference.class.getName(),
			new String[0],
			Collections.emptyList(),
			1,
			ServiceScope.NONE);

		Collection<ReferenceModel> references = componentModel.getReferences();

		Iterator<ReferenceModel> refIter = references.iterator();

		assertReferenceModel(
			refIter.next(),
			BarReference.class,
			ReferenceCardinality.MANDATORY,
			CollectionType.SERVICE,
			BarReference.class,
			"barReference",
			ReferencePolicy.STATIC,
			ReferencePolicyOption.RELUCTANT,
			ReferenceScope.BUNDLE,
			BarReference.class,
			"",
			Sets.hashSet(BarReference.class, Object.class));

		componentModel = compItr.next();

		assertComponentModel(
			componentModel,
			FooWithReference.class,
			0,
			FooWithReference.class.getName(),
			new String[0],
			Collections.emptyList(),
			1,
			ServiceScope.NONE);

		references = componentModel.getReferences();

		refIter = references.iterator();

		assertReferenceModel(
			refIter.next(),
			FooReference.class,
			ReferenceCardinality.MANDATORY,
			CollectionType.SERVICE,
			FooReference.class,
			"fooReference",
			ReferencePolicy.STATIC,
			ReferencePolicyOption.RELUCTANT,
			ReferenceScope.BUNDLE,
			FooReference.class,
			"",
			Sets.hashSet(FooReference.class, Object.class));
	}

	@Test
	public void testAnnotatedBeans() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(Collections.singletonList("OSGI-INF/cdi/beans-annotated.xml"), null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = new ContainerState(null, null) {

			@Override
			public BeansModel beansModel() {
				return beansModel;
			}

			public <T extends ResourceLoader & ProxyServices> T loader() {
				return null;
			}

		};

		ContainerDiscovery.discover(containerState);

		Collection<ComponentModel> componentModels = sort(
			beansModel.getComponentModels(), (a, b) -> a.getName().compareTo(b.getName()));

		assertEquals(2, componentModels.size());

		Iterator<ComponentModel> compItr = componentModels.iterator();

		ComponentModel componentModel = compItr.next();

		assertComponentModel(
			componentModel,
			FooAnnotated.class,
			0,
			"foo.annotated",
			new String[] {"service.ranking:Integer=12"},
			Arrays.asList(Foo.class.getName()),
			0,
			ServiceScope.SINGLETON);

		componentModel = compItr.next();

		assertComponentModel(
			componentModel,
			BarAnnotated.class,
			1,
			BarAnnotated.class.getName(),
			new String[0],
			Collections.emptyList(),
			6,
			ServiceScope.NONE);

		Collection<ReferenceModel> references = sort(componentModel.getReferences(), (a, b) -> a.getName().compareTo(b.getName()));

		Iterator<ReferenceModel> refIter = references.iterator();

		assertReferenceModel(
			refIter.next(),
			Collection.class,
			ReferenceCardinality.MULTIPLE,
			CollectionType.SERVICE,
			new TypeReference<Collection<Foo>>() {}.getType(),
			"collectionFoos",
			ReferencePolicy.DYNAMIC,
			ReferencePolicyOption.RELUCTANT,
			ReferenceScope.DEFAULT,
			Foo.class,
			"",
			Sets.hashSet(new TypeReference<Collection<Foo>>() {}.getType(), Object.class));

		assertReferenceModel(
			refIter.next(),
			Foo.class,
			ReferenceCardinality.OPTIONAL,
			CollectionType.SERVICE,
			Foo.class,
			"foo",
			ReferencePolicy.STATIC,
			ReferencePolicyOption.RELUCTANT,
			ReferenceScope.DEFAULT,
			Foo.class,
			"",
			Sets.hashSet(Foo.class, Object.class));

		assertReferenceModel(
			refIter.next(),
			Foo.class,
			ReferenceCardinality.MULTIPLE,
			CollectionType.SERVICE,
			Foo.class,
			"foos",
			ReferencePolicy.STATIC,
			ReferencePolicyOption.RELUCTANT,
			ReferenceScope.DEFAULT,
			Foo.class,
			"",
			Sets.hashSet(Foo.class, Object.class));

		assertReferenceModel(
			refIter.next(),
			Collection.class,
			ReferenceCardinality.MULTIPLE,
			CollectionType.PROPERTIES,
			new TypeReference<Collection<Map<String, Object>>>(){}.getType(),
			"propertiesFoos",
			ReferencePolicy.STATIC,
			ReferencePolicyOption.RELUCTANT,
			ReferenceScope.DEFAULT,
			Foo.class,
			"",
			Sets.hashSet(new TypeReference<Collection<Map<String, Object>>>(){}.getType(), Object.class));

		assertReferenceModel(
			refIter.next(),
			Collection.class,
			ReferenceCardinality.MULTIPLE,
			CollectionType.REFERENCE,
			new TypeReference<Collection<ServiceReference<Foo>>>(){}.getType(),
			"serviceReferencesFoos",
			ReferencePolicy.STATIC,
			ReferencePolicyOption.RELUCTANT,
			ReferenceScope.PROTOTYPE,
			Foo.class,
			"",
			Sets.hashSet(new TypeReference<Collection<ServiceReference<Foo>>>(){}.getType(), Object.class));

		assertReferenceModel(
			refIter.next(),
			Collection.class,
			ReferenceCardinality.MULTIPLE,
			CollectionType.TUPLE,
			new TypeReference<Collection<Map.Entry<Map<String, Object>, Foo>>>(){}.getType(),
			"tupleFoos",
			ReferencePolicy.STATIC,
			ReferencePolicyOption.GREEDY,
			ReferenceScope.DEFAULT,
			Foo.class,
			"",
			Sets.hashSet(new TypeReference<Collection<Map.Entry<Map<String, Object>, Foo>>>(){}.getType(), Object.class));
	}

	@Test(expected = DefinitionException.class)
	public void testDescriptorError1() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(Collections.singletonList("OSGI-INF/cdi/beans-badly-annotated.xml"), null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = new ContainerState(null, null) {

			@Override
			public BeansModel beansModel() {
				return beansModel;
			}

			public <T extends ResourceLoader & ProxyServices> T loader() {
				return null;
			}

		};

		ContainerDiscovery.discover(containerState);
	}

	protected void assertComponentModel(
		ComponentModel componentModel,
		Class<?> beanClass,
		int confSize,
		String name,
		String[] properties,
		List<String> provides,
		int refSize,
		ServiceScope scope) {

		assertEquals(beanClass, componentModel.getBeanClass());
		assertEquals(confSize, componentModel.getConfigurations().size());
		assertEquals(name, componentModel.getName());
		assertArrayEquals(properties, componentModel.getProperties());
		assertEquals(provides, componentModel.getProvides());
		assertEquals(refSize, componentModel.getReferences().size());
		assertEquals(scope, componentModel.getServiceScope());
	}

	protected void assertConfigurationModel(
		ConfigurationModel configurationModel,
		String[] pid,
		ConfigurationPolicy policy,
		Type type) {

		assertArrayEquals(pid, configurationModel.getPid());
		assertEquals(policy, configurationModel.getConfigurationPolicy());
		assertEquals(type, configurationModel.getType());
	}

	private void assertReferenceModel(
		ReferenceModel referenceModel,
		Class<?> beanClass,
		ReferenceCardinality cardinality,
		CollectionType collectionType,
		Type injectionPointType,
		String name,
		ReferencePolicy policy,
		ReferencePolicyOption option,
		ReferenceScope scope,
		Class<?> serviceClass,
		String target,
		Set<Type> types) {

		assertEquals(beanClass, referenceModel.getBeanClass());
		assertEquals(cardinality, referenceModel.getCardinality());
		assertEquals(collectionType, referenceModel.getCollectionType());
		assertEquals(injectionPointType, referenceModel.getInjectionPointType());
		assertEquals(name, referenceModel.getName());
		assertEquals(policy, referenceModel.getPolicy());
		assertEquals(option, referenceModel.getPolicyOption());
		assertEquals(scope, referenceModel.getScope());
		assertEquals(serviceClass, referenceModel.getServiceClass());
		assertEquals(target, referenceModel.getTarget());
		assertEquals(types, referenceModel.getTypes());
	}
*/
}
