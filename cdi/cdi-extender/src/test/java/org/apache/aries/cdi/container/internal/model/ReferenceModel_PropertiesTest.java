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
import java.util.Map;
import java.util.Optional;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.apache.aries.cdi.container.test.AbstractTestBase;
import org.apache.aries.cdi.container.test.MockInjectionPoint;
import org.junit.Test;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.util.converter.TypeReference;

public class ReferenceModel_PropertiesTest extends AbstractTestBase {

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceDefined() throws Exception {
		class C {
			@Inject
			@Reference
			public Map<String, Object> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void withServiceDefined() throws Exception {
		Type type = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Map<String, Object> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void withServiceDefined_b() throws Exception {
		Type type = new TypeReference<
			Map<String, ?>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Map<String, ?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void withServiceDefined_optional() throws Exception {
		Type type = new TypeReference<
			Optional<Map<String, Object>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Optional<Map<String, Object>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(Optional.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined() throws Exception {
		class C {
			@Inject
			@Reference
			public Collection<Map<String, Object>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined_b() throws Exception {
		class C {
			@Inject
			@Reference
			public Collection<Map<String, ?>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void collectionWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, Object>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Collection<Map<String, Object>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void collectionWithServiceDefined_b() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, ?>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Collection<Map<String, ?>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
		class C {
			@Inject
			@Reference
			public List<Map<String, Object>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceDefined_b() throws Exception {
		class C {
			@Inject
			@Reference
			public List<Map<String, ?>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void listWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<Map<String, Object>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public List<Map<String, Object>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

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
	public void listWithServiceDefined_b() throws Exception {
		Type type = new TypeReference<
			List<Map<String, ?>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public List<Map<String, ?>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder().injectionPoint(injectionPoint).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	// params

	@Test(expected = IllegalArgumentException.class)
	public void p_withoutServiceDefined() throws Exception {
		class C {
			@Inject
			public void set(@Reference Map<String, Object> m) {}
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Map.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void p_withServiceDefined() throws Exception {
		Type type = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Map<String, Object> m){};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Map.class).getParameters()[0]);

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
	public void p_withServiceDefined_optional() throws Exception {
		Type type = new TypeReference<
			Optional<Map<String, Object>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Optional<Map<String, Object>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Optional.class).getParameters()[0]);

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
	public void p_withServiceDefined_b() throws Exception {
		Type type = new TypeReference<
			Map<String, ?>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Map<String, ?> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Map.class).getParameters()[0]);

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
	public void p_collectionWithoutServiceDefined() throws Exception {
		class C {
			@Inject
			public void set(@Reference Collection<Map<String, Object>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_collectionWithoutServiceDefined_b() throws Exception {
		class C {
			@Inject
			public void set(@Reference Collection<Map<String, ?>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void p_collectionWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, Object>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Collection<Map<String, Object>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

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
	public void p_collectionWithServiceDefined_b() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, ?>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) Collection<Map<String, ?>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", Collection.class).getParameters()[0]);

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
	public void p_listWithoutServiceDefined() throws Exception {
		class C {
			@Inject
			public void set(@Reference List<Map<String, Object>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void p_listWithoutServiceDefined_b() throws Exception {
		class C {
			@Inject
			public void set(@Reference List<Map<String, ?>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

		new ReferenceModel.Builder().injectionPoint(injectionPoint).build();
	}

	@Test
	public void p_listWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<Map<String, Object>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) List<Map<String, Object>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

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
	public void p_listWithServiceDefined_b() throws Exception {
		Type type = new TypeReference<
			List<Map<String, ?>>
		>(){}.getType();

		class C {
			@Inject
			public void set(@Reference(Integer.class) List<Map<String, ?>> m) {};
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getMethod("set", List.class).getParameters()[0]);

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