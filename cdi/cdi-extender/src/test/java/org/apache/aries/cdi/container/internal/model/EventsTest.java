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

public class EventsTest {

/*	@Test
	public void test_Event() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", ObserverFoo.class.getName())) {

			ContainerState containerState = container.containerState();

			BeansModel beansModel = containerState.beansModel();

			Collection<ComponentModel> models = sort(beansModel.getComponentModels());

			assertEquals(1, models.size());

			assertEquals(CdiEvent.Type.CREATED, containerState.lastState());

			BeanManager beanManager = containerState.getBeanManager();
			Set<Bean<?>> beans = beanManager.getBeans(ObserverFoo.class, AnyLiteral.INSTANCE);
			@SuppressWarnings("rawtypes")
			Bean bean = beanManager.resolve(beans);
			@SuppressWarnings("unchecked")

			javax.enterprise.context.spi.Context context = beanManager.getContext(bean.getScope());
			ObserverFoo observerFoo = (ObserverFoo)context.get(bean, beanManager.createCreationalContext(bean));

			assertEquals(0, observerFoo.foos().size());
			assertEquals(CdiEvent.Type.CREATED, containerState.lastState());

			ComponentModel componentModel = models.iterator().next();
			List<ReferenceModel> references = componentModel.getReferences();
			assertEquals(1, references.size());

			Map<String, ReferenceCallback> map = containerState.referenceCallbacks().get(componentModel);
			assertEquals(1, map.size());
			ReferenceCallback callback = map.get(references.iterator().next().getName());
			assertNotNull(callback);

			MockServiceReference<Object> mockServiceReference = new MockServiceReference<Object>(new Foo() {});

			callback.addingService(mockServiceReference);

			assertEquals(1, observerFoo.foos().size());

			mockServiceReference.setProperty(Constants.SERVICE_RANKING, 1);

			callback.modifiedService(mockServiceReference, mockServiceReference.getService());

			assertEquals(1, observerFoo.foos().size());

			callback.removedService(mockServiceReference, mockServiceReference.getService());

			assertEquals(0, observerFoo.foos().size());
		}
	}
*/
}
