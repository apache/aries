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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.ReferenceModel;
import org.apache.aries.cdi.container.test.MockInjectionPoint;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.junit.Test;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.reference.ReferenceServiceObjects;
import org.osgi.util.converter.TypeReference;

public class ReferenceModel_ReferenceServiceObjectsTest {

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceDefined_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public ReferenceServiceObjects m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			@Reference
			public ReferenceServiceObjects<?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void withoutServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			ReferenceServiceObjects<Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public ReferenceServiceObjects<Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(ReferenceServiceObjects.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceDefined_raw() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			ReferenceServiceObjects
		>(){}.getType();

		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference(Integer.class)
			public ReferenceServiceObjects m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(ReferenceServiceObjects.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			ReferenceServiceObjects<?>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public ReferenceServiceObjects<?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(ReferenceServiceObjects.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			ReferenceServiceObjects<Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public ReferenceServiceObjects<Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(ReferenceServiceObjects.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public ReferenceServiceObjects<Foo> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public Collection<ReferenceServiceObjects> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			@Reference
			public Collection<ReferenceServiceObjects<?>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void collectionWithoutServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			Collection<ReferenceServiceObjects<Integer>>
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public Collection<ReferenceServiceObjects<Integer>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void collectionWithServiceDefined_raw() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Collection<ReferenceServiceObjects>
		>(){}.getType();

		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference(Integer.class)
			public Collection<ReferenceServiceObjects> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}
	@Test
	public void collectionWithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<ReferenceServiceObjects<?>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Collection<ReferenceServiceObjects<?>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}


	@Test
	public void collectionWithServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			Collection<ReferenceServiceObjects<Integer>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Collection<ReferenceServiceObjects<Integer>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public Collection<ReferenceServiceObjects<Foo>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceDefined_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public List<ReferenceServiceObjects> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			@Reference
			public List<ReferenceServiceObjects<?>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void listWithoutServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			List<ReferenceServiceObjects<Integer>>
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public List<ReferenceServiceObjects<Integer>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void listWithServiceDefined_raw() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			List<ReferenceServiceObjects>
		>(){}.getType();

		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference(Integer.class)
			public List<ReferenceServiceObjects> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void listWithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			List<ReferenceServiceObjects<?>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public List<ReferenceServiceObjects<?>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void listWithServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			List<ReferenceServiceObjects<Integer>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public List<ReferenceServiceObjects<Integer>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public List<ReferenceServiceObjects<Foo>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithoutServiceDefined() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public Instance<ReferenceServiceObjects> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithoutServiceDefined_typed() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public Instance<ReferenceServiceObjects> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	///////////////////////////////////////////////////////////////////////////////



	// parameters

	@Test(expected = IllegalArgumentException.class)
	public void p_withoutServiceDefined_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			public void set(@Reference ReferenceServiceObjects m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", ReferenceServiceObjects.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_withoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			public void set(@Reference ReferenceServiceObjects<?> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", ReferenceServiceObjects.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void p_withoutServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			ReferenceServiceObjects<Integer>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference ReferenceServiceObjects<Integer> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", ReferenceServiceObjects.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(ReferenceServiceObjects.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void p_withServiceDefined_raw() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			ReferenceServiceObjects
		>(){}.getType();

		class C {
			@Inject
			public void set(@SuppressWarnings("rawtypes") @Reference(Integer.class) ReferenceServiceObjects m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", ReferenceServiceObjects.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(ReferenceServiceObjects.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void p_withServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			ReferenceServiceObjects<?>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) ReferenceServiceObjects<?> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", ReferenceServiceObjects.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(ReferenceServiceObjects.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void p_withServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			ReferenceServiceObjects<Integer>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) ReferenceServiceObjects<Integer> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", ReferenceServiceObjects.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(ReferenceServiceObjects.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_withServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			public void set(@Reference(Integer.class) ReferenceServiceObjects<Foo> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", ReferenceServiceObjects.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_collectionWithoutServiceDefined_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			public void set(@Reference Collection<ReferenceServiceObjects> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_collectionWithoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			public void set(@Reference Collection<ReferenceServiceObjects<?>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void p_collectionWithoutServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			Collection<ReferenceServiceObjects<Integer>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference Collection<ReferenceServiceObjects<Integer>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void p_collectionWithServiceDefined_raw() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Collection<ReferenceServiceObjects>
		>(){}.getType();

		class C {
			@Inject
			public void set(@SuppressWarnings("rawtypes") @Reference(Integer.class) Collection<ReferenceServiceObjects> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void p_collectionWithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<ReferenceServiceObjects<?>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Collection<ReferenceServiceObjects<?>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void p_collectionWithServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			Collection<ReferenceServiceObjects<Integer>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Collection<ReferenceServiceObjects<Integer>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_collectionWithServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			public void set(@Reference(Integer.class) Collection<ReferenceServiceObjects<Foo>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_listWithoutServiceDefined_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			public void set(@Reference List<ReferenceServiceObjects> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_listWithoutServiceDefined_wildcard() throws Exception {
		class C {
			@Inject
			public void set(@Reference List<ReferenceServiceObjects<?>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void p_listWithoutServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			List<ReferenceServiceObjects<Foo>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference List<ReferenceServiceObjects<Foo>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void p_listWithServiceDefined_raw() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			List<ReferenceServiceObjects>
		>(){}.getType();

		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			public void set(@Reference(Integer.class) List<ReferenceServiceObjects> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void p_listWithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			List<ReferenceServiceObjects<?>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) List<ReferenceServiceObjects<?>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test
	public void p_listWithServiceDefined_typed() throws Exception {
		Type type = new TypeReference<
			List<ReferenceServiceObjects<Integer>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) List<ReferenceServiceObjects<Integer>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.SERVICEOBJECTS, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_listWithServiceDefined_wrongtype() throws Exception {
		class C {
			@Inject
			public void set(@Reference(Integer.class) List<ReferenceServiceObjects<Foo>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_instanceWithoutServiceDefined() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			public void set(@Reference Instance<ReferenceServiceObjects> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Instance.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_instanceWithoutServiceDefined_typed() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			public void set(@Reference Instance<ReferenceServiceObjects> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Instance.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

}
