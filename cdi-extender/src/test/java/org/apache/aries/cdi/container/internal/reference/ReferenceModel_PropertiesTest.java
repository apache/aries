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
import java.util.Optional;

import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.apache.aries.cdi.container.test.MockInjectionPoint;
import org.junit.Test;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.util.converter.TypeReference;

public class ReferenceModel_PropertiesTest {

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void withServiceDefined() throws Exception {
		Type type = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Map.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceDefined_cardinality() throws Exception {
		Type type = new TypeReference<
			Optional<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Optional.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceDefinedAsWildCard() throws Exception {
		Type type = new TypeReference<
			Map<String, ?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Map.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined_Wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, ?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void collectionWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test
	public void collectionWithServiceDefined_cardinality() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test
	public void collectionWithServiceDefined_Wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, ?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceDefined_Wildcard() throws Exception {
		Type type = new TypeReference<
			List<Map<String, ?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void listWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test
	public void listWithServiceDefined_cardinality() throws Exception {
		Type type = new TypeReference<
			List<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test
	public void listWithServiceDefined_Wildcard() throws Exception {
		Type type = new TypeReference<
			List<Map<String, ?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}
}