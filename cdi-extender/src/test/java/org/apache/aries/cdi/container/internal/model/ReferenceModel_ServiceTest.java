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

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.apache.aries.cdi.container.test.AbstractTestBase;
import org.apache.aries.cdi.container.test.MockInjectionPoint;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.junit.Test;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.util.converter.TypeReference;

public class ReferenceModel_ServiceTest extends AbstractTestBase {

	@Test
	public void withoutServiceDefined_raw() throws Exception {
		Type type = new TypeReference<
			Integer
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public Integer m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Integer.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			@Reference
			public Callable<?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void withoutServiceDefined_optional() throws Exception {
		Type type = new TypeReference<
			Optional<Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public Optional<Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Optional.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceDefined_raw() throws Exception {
		Type type = new TypeReference<
			Integer
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Integer m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Integer.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			@Reference(Callable.class)
			public Callable<?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void withServiceDefined_optional() throws Exception {
		Type type = new TypeReference<
			Optional<?>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Optional<?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void withServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			@Reference(Foo.class)
			public Integer m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceDefined_optional_wrongtype() throws Exception {
		class C {
			@Inject
			@Reference(Foo.class)
			public Optional<Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionwithoutServiceDefined_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public Collection m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionwithoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			@Reference
			public Collection<?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void collectionwithoutServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			Collection<Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public Collection<Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void collectionwithServiceDefined_raw() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Collection
		>(){}.getType();

		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference(Integer.class)
			public Collection m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void collectionwithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<?>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Collection<?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void collectionwithServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			Collection<Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Collection<Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void collectionwithServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public Collection<Foo> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void listwithoutServiceDefined_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public List m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void listwithoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			@Reference
			public List<?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void listwithoutServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			List<Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public List<Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void listwithServiceDefined_raw() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			List
		>(){}.getType();

		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference(Integer.class)
			public List m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void listwithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			List<?>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public List<?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void listwithServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			List<Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public List<Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void listwithServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public List<Foo> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	// params

	@Test
	public void p_withoutServiceDefined_raw() throws Exception {
		Type type = new TypeReference<
			Integer
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference Integer m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Integer.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Integer.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_withoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			public void set(@Reference Callable<?> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Callable.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void p_withoutServiceDefined_optional() throws Exception {
		Type type = new TypeReference<
			Optional<Integer>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference Optional<Integer> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Optional.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Optional.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test
	public void p_withServiceDefined_raw() throws Exception {
		Type type = new TypeReference<
			Integer
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Integer m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Integer.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Integer.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_withServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			public void set(@Reference(Callable.class) Callable<?> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Callable.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void p_withServiceDefined_optional() throws Exception {
		Type type = new TypeReference<
			Optional<?>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Optional<?> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Optional.class).getParameters()[0]);

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
	public void p_withServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			public void set(@Reference(Foo.class) Integer m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Integer.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_withServiceDefined_optional_wrongtype() throws Exception {
		class C {
			@Inject
			public void set(@Reference(Foo.class) Optional<Integer> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Optional.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_collectionwithoutServiceDefined_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			public void set(@Reference Collection m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_collectionwithoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			public void set(@Reference Collection<?> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void p_collectionwithoutServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			Collection<Integer>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference Collection<Integer> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

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
	public void p_collectionwithServiceDefined_raw() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Collection
		>(){}.getType();

		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			public void set(@Reference(Integer.class) Collection m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

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
	public void p_collectionwithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<?>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Collection<?> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

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
	public void p_collectionwithServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			Collection<Integer>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Collection<Integer> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

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
	public void p_collectionwithServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			public void set(@Reference(Integer.class) Collection<Foo> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_listwithoutServiceDefined_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			public void set(@Reference List m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_listwithoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			public void set(@Reference List<?> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void p_listwithoutServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			List<Integer>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference List<Integer> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

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
	public void p_listwithServiceDefined_raw() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			List
		>(){}.getType();

		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			public void set(@Reference(Integer.class) List m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

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
	public void p_listwithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			List<?>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) List<?> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

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
	public void p_listwithServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			List<Integer>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) List<Integer> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

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
	public void p_listwithServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			public void set(@Reference(Integer.class) List<Foo> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}
}