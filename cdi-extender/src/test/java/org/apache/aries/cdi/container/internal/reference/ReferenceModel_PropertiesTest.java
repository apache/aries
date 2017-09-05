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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel;
import org.apache.aries.cdi.container.test.MockInjectionPoint;
import org.junit.Test;
import org.osgi.service.cdi.annotations.ReferenceCardinality;
import org.osgi.util.converter.TypeReference;

public class ReferenceModel_PropertiesTest {

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test
	public void withServiceDefined() throws Exception {
		Type type = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(Map.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceDefined_cardinality() throws Exception {
		Type type = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.OPTIONAL).build();

		assertEquals(Map.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.OPTIONAL, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceDefined_cardinality_multiple() throws Exception {
		Type type = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.MULTIPLE).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceDefined_cardinality_atleastone() throws Exception {
		Type type = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.AT_LEAST_ONE).build();
	}

	@Test
	public void withServiceDefinedAsWildCard() throws Exception {
		Type type = new TypeReference<
			Map<String, ?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(Map.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined_Wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, ?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test
	public void collectionWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test
	public void collectionWithServiceDefined_cardinality() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.AT_LEAST_ONE).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.AT_LEAST_ONE, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithServiceDefined_cardinality_mandatory() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.MANDATORY).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithServiceDefined_cardinality_optional() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.OPTIONAL).build();
	}

	@Test
	public void collectionWithServiceDefined_Wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<Map<String, ?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceDefined_Wildcard() throws Exception {
		Type type = new TypeReference<
			List<Map<String, ?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test
	public void listWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test
	public void listWithServiceDefined_cardinality() throws Exception {
		Type type = new TypeReference<
			List<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.AT_LEAST_ONE).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.AT_LEAST_ONE, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithServiceDefined_cardinality_mandatory() throws Exception {
		Type type = new TypeReference<
			List<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.MANDATORY).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithServiceDefined_cardinality_optional() throws Exception {
		Type type = new TypeReference<
			List<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.OPTIONAL).build();
	}

	@Test
	public void listWithServiceDefined_Wildcard() throws Exception {
		Type type = new TypeReference<
			List<Map<String, ?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			Instance<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithoutServiceDefined_Wildcard() throws Exception {
		Type type = new TypeReference<
			Instance<Map<String, ?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test
	public void instanceWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Instance<Map<String, Object>>
		>(){}.getType();
		Type injectionPointType = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(Map.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(injectionPointType, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test
	public void instanceWithServiceDefined_cardinality() throws Exception {
		Type type = new TypeReference<
			Instance<Map<String, Object>>
		>(){}.getType();
		Type injectionPointType = new TypeReference<
			Map<String, Object>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.AT_LEAST_ONE).build();

		assertEquals(Map.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(injectionPointType, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.AT_LEAST_ONE, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithServiceDefined_cardinality_mandatory() throws Exception {
		Type type = new TypeReference<
			Instance<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.MANDATORY).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithServiceDefined_cardinality_optional() throws Exception {
		Type type = new TypeReference<
			Instance<Map<String, Object>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).cardinality(ReferenceCardinality.OPTIONAL).build();
	}

	@Test
	public void instanceWithServiceDefined_Wildcard() throws Exception {
		Type type = new TypeReference<
			Instance<Map<String, ?>>
		>(){}.getType();
		Type injectionPointType = new TypeReference<
			Map<String, ?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(Map.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(injectionPointType, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.PROPERTIES, referenceModel.getCollectionType());
	}

}