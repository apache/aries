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

package org.apache.aries.cdi.container.internal.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.apache.aries.cdi.container.test.MockInjectionPoint;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.junit.Test;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.util.converter.TypeReference;

public class ReferenceModel_TupleTest {

	@Test
	public void withServiceType() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, ?>, Integer>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Map.Entry.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_A() throws Exception {
		Type type = new TypeReference<
			Map.Entry<?, Integer>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_B() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Collection<?>, Callable<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_C() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Map.Entry<Map, Callable<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_D() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<?, Foo>, Callable<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_E() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, Foo>, Callable<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_F() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, ? extends Foo>, Callable<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Map.Entry
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceDefinedButGenericTuple() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, ?>, ?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceDefinedButNotAssignable() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, ?>, String>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void withServiceDefined() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, ?>, Number>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Map.Entry.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Collection<Map.Entry>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void collectionWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<Map.Entry<Map<String, ?>, Number>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			List<Map.Entry>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void listWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<Map.Entry<Map<String, ?>, Number>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithoutServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Instance<Map.Entry>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Instance<Map.Entry<Map<String, ?>, Number>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	///////////////////////////////////////////////////////////////////////////////

	@Test
	public void typed_withoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, Object>, Foo>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Map.Entry.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void typed_withServiceDefined() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, Object>, Foo>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void typed_collectionWithoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<Map.Entry<Map<String, Object>, Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void typed_collectionWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<Map.Entry<Map<String, Object>, Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void typed_listWithoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<Map.Entry<Map<String, Object>, Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void typed_listWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<Map.Entry<Map<String, Object>, Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void typed_instanceWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Instance<Map.Entry<Map<String, Object>, Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}
}