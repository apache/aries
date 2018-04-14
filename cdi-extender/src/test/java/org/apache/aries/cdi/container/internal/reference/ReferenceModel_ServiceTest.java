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
import java.util.Optional;
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

public class ReferenceModel_ServiceTest {

	@Test
	public void single() throws Exception {
		Type type = new TypeReference<
			Integer
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Integer.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test
	public void single_cardinality() throws Exception {
		Type type = new TypeReference<
			Optional<Integer>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Optional.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void single_incompatibleSpecified() throws Exception {
		Type type = new TypeReference<
			Integer
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Foo.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void collection() throws Exception {
		Type type = new TypeReference<
			Collection<Foo>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test
	public void collection_cardinality() throws Exception {
		Type type = new TypeReference<
			Collection<Integer>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void collection_cardinality_optional() throws Exception {
		Type type = new TypeReference<
			Collection<Callable<String>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collection_incompatibleSpecified() throws Exception {
		Type type = new TypeReference<
			Collection<Callable<String>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Foo.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collection_Wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void collection_specified_Wilcard() throws Exception {
		Type type = new TypeReference<
			Collection<Integer>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test
	public void list() throws Exception {
		Type type = new TypeReference<
			List<Integer>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test
	public void list_cardinality() throws Exception {
		Type type = new TypeReference<
			List<Integer>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void list_cardinality_mandatory() throws Exception {
		Type type = new TypeReference<
			List<Callable<String>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void list_cardinality_optional() throws Exception {
		Type type = new TypeReference<
			List<Callable<String>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void list_incompatibleSpecified() throws Exception {
		Type type = new TypeReference<
			List<Callable<String>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Foo.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void list_Wildcard() throws Exception {
		Type type = new TypeReference<
			List<?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void list_specified_wildcard() throws Exception {
		Type type = new TypeReference<
			List<?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Integer.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instance_cardinality_mandatory() throws Exception {
		Type type = new TypeReference<
			Instance<Callable<String>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instance_cardinality_optional() throws Exception {
		Type type = new TypeReference<
			Instance<Callable<String>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instance_incompatibleSpecified() throws Exception {
		Type type = new TypeReference<
			Instance<Callable<String>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type, Sets.hashSet(Reference.Literal.of(Foo.class, "")));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instance_Wildcard() throws Exception {
		Type type = new TypeReference<
			Instance<?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}
}