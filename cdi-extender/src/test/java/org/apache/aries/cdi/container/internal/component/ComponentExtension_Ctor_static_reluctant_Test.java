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

package org.apache.aries.cdi.container.internal.component;

import static org.apache.aries.cdi.container.test.TestUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel;
import org.apache.aries.cdi.container.test.MockCdiContainer;
import org.apache.aries.cdi.container.test.MockCdiContainerAndComponents;
import org.apache.aries.cdi.container.test.beans.Bar;
import org.apache.aries.cdi.container.test.beans.CtorArrayListFoo;
import org.apache.aries.cdi.container.test.beans.CtorFoo;
import org.apache.aries.cdi.container.test.beans.CtorFooBar;
import org.apache.aries.cdi.container.test.beans.CtorFooFoo;
import org.apache.aries.cdi.container.test.beans.CtorFooFooNamed;
import org.apache.aries.cdi.container.test.beans.CtorFooOptional;
import org.apache.aries.cdi.container.test.beans.CtorListFoo;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.apache.aries.cdi.container.test.beans.CtorCollectionFoo;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.service.cdi.annotations.ReferenceCardinality;
import org.osgi.service.cdi.annotations.ReferencePolicy;
import org.osgi.service.cdi.annotations.ReferencePolicyOption;

public class ComponentExtension_Ctor_static_reluctant_Test {

	@Test
	public void test_CtorArrayListFoo() throws Exception {
		try (MockCdiContainerAndComponents container =
				new MockCdiContainerAndComponents(
					"test", CtorArrayListFoo.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<ComponentModel> models = sort(beansModel.getComponentModels());

			Assert.assertEquals(1, models.size());

//			List<ReferenceModel> references = container.getTrackedReferences();
//
//			Assert.assertEquals(1, references.size());
//
//			ReferenceModel referenceModel = references.get(0);
//
//			Assert.assertEquals(ArrayList.class, referenceModel.getBeanClass());
//			Assert.assertEquals(
//				ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
//			Assert.assertEquals("Foo0", referenceModel.getName());
//			Assert.assertEquals(ReferencePolicy.STATIC, referenceModel.getPolicy());
//			Assert.assertEquals(
//				ReferencePolicyOption.RELUCTANT, referenceModel.getPolicyOption());
//			Assert.assertNull(referenceModel.getScope());
//			Assert.assertEquals(Foo.class, referenceModel.getServiceClass());
//			Assert.assertEquals("", referenceModel.getTarget());
		}
	}

	@Test
	public void test_CtorCollectionFoo() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorCollectionFoo.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<ComponentModel> models = sort(beansModel.getComponentModels());

			Assert.assertEquals(1, models.size());

//			List<ReferenceModel> references = container.getTrackedReferences();
//
//			Assert.assertEquals(1, references.size());
//
//			ReferenceModel referenceModel = references.get(0);
//
//			Assert.assertEquals(Collection.class, referenceModel.getBeanClass());
//			Assert.assertEquals(
//				ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
//			Assert.assertEquals("Foo0", referenceModel.getName());
//			Assert.assertEquals(ReferencePolicy.STATIC, referenceModel.getPolicy());
//			Assert.assertEquals(
//				ReferencePolicyOption.RELUCTANT, referenceModel.getPolicyOption());
//			Assert.assertNull(referenceModel.getScope());
//			Assert.assertEquals(Foo.class, referenceModel.getServiceClass());
//			Assert.assertEquals("", referenceModel.getTarget());
		}
	}

	@Test
	public void test_CtorFooFoo() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorFooFoo.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<ComponentModel> models = sort(beansModel.getComponentModels());

			Assert.assertEquals(1, models.size());

//			List<ReferenceModel> references = container.getTrackedReferences();
//
//			Assert.assertEquals(2, references.size());
//
//			ReferenceModel referenceModel = references.get(0);
//
//			Assert.assertEquals(Foo.class, referenceModel.getBeanClass());
//			Assert.assertEquals(
//				ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
//			Assert.assertEquals("Foo0", referenceModel.getName());
//			Assert.assertEquals(ReferencePolicy.STATIC, referenceModel.getPolicy());
//			Assert.assertEquals(
//				ReferencePolicyOption.RELUCTANT, referenceModel.getPolicyOption());
//			Assert.assertNull(referenceModel.getScope());
//			Assert.assertEquals(Foo.class, referenceModel.getServiceClass());
//			Assert.assertEquals("", referenceModel.getTarget());
		}
	}

	@Test
	public void test_CtorFoo() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorFoo.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<ComponentModel> models = sort(beansModel.getComponentModels());

			Assert.assertEquals(1, models.size());

//			List<ReferenceModel> references = container.getTrackedReferences();
//
//			Assert.assertEquals(1, references.size());
//
//			ReferenceModel referenceModel = references.get(0);
//
//			Assert.assertEquals(Foo.class, referenceModel.getBeanClass());
//			Assert.assertEquals(
//				ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
//			Assert.assertEquals("Foo0", referenceModel.getName());
//			Assert.assertEquals(ReferencePolicy.STATIC, referenceModel.getPolicy());
//			Assert.assertEquals(
//				ReferencePolicyOption.RELUCTANT, referenceModel.getPolicyOption());
//			Assert.assertNull(referenceModel.getScope());
//			Assert.assertEquals(Foo.class, referenceModel.getServiceClass());
//			Assert.assertEquals("", referenceModel.getTarget());
		}
	}

	@Test
	public void test_CtorListFoo() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorListFoo.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<ComponentModel> models = sort(beansModel.getComponentModels());

			Assert.assertEquals(1, models.size());

//			List<ReferenceModel> references = container.getTrackedReferences();
//
//			Assert.assertEquals(1, references.size());
//
//			ReferenceModel referenceModel = references.get(0);
//
//			Assert.assertEquals(List.class, referenceModel.getBeanClass());
//			Assert.assertEquals(
//				ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
//			Assert.assertEquals("Foo0", referenceModel.getName());
//			Assert.assertEquals(ReferencePolicy.STATIC, referenceModel.getPolicy());
//			Assert.assertEquals(
//				ReferencePolicyOption.RELUCTANT, referenceModel.getPolicyOption());
//			Assert.assertNull(referenceModel.getScope());
//			Assert.assertEquals(Foo.class, referenceModel.getServiceClass());
//			Assert.assertEquals("", referenceModel.getTarget());
		}
	}

	@Test
	public void test_CtorFooBar() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorFooBar.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<ComponentModel> models = sort(beansModel.getComponentModels());

			Assert.assertEquals(1, models.size());

//			List<ReferenceModel> references = container.getTrackedReferences();
//
//			Assert.assertEquals(2, references.size());
//
//			ReferenceModel referenceModel = references.get(0);
//
//			Assert.assertEquals(Foo.class, referenceModel.getBeanClass());
//			Assert.assertEquals(
//				ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
//			Assert.assertEquals("Foo0", referenceModel.getName());
//			Assert.assertEquals(ReferencePolicy.STATIC, referenceModel.getPolicy());
//			Assert.assertEquals(
//				ReferencePolicyOption.RELUCTANT, referenceModel.getPolicyOption());
//			Assert.assertNull(referenceModel.getScope());
//			Assert.assertEquals(Foo.class, referenceModel.getServiceClass());
//			Assert.assertEquals("", referenceModel.getTarget());

//			referenceModel = references.get(1);
//
//			Assert.assertEquals(Bar.class, referenceModel.getBeanClass());
//			Assert.assertEquals(
//				ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
//			Assert.assertEquals("Bar1", referenceModel.getName());
//			Assert.assertEquals(ReferencePolicy.STATIC, referenceModel.getPolicy());
//			Assert.assertEquals(
//				ReferencePolicyOption.RELUCTANT, referenceModel.getPolicyOption());
//			Assert.assertNull(referenceModel.getScope());
//			Assert.assertEquals(Bar.class, referenceModel.getServiceClass());
//			Assert.assertEquals("", referenceModel.getTarget());
		}
	}

	@Test
	public void test_CtorFooFooNamed() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorFooFooNamed.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<ComponentModel> models = sort(beansModel.getComponentModels());

			Assert.assertEquals(1, models.size());

//			Collection<ReferenceModel> references = sort(container.getTrackedReferences(), (c1, c2) -> c1.toString().compareTo(c2.toString()));
//
//			Assert.assertEquals(2, references.size());
//
//			Iterator<ReferenceModel> referenceIterator = references.iterator();
//
//			ReferenceModel referenceModel = referenceIterator.next();
//
//			Assert.assertEquals(Foo.class, referenceModel.getBeanClass());
//			Assert.assertEquals(
//				ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
//			Assert.assertEquals("foo_a", referenceModel.getName());
//			Assert.assertEquals(ReferencePolicy.STATIC, referenceModel.getPolicy());
//			Assert.assertEquals(
//				ReferencePolicyOption.RELUCTANT, referenceModel.getPolicyOption());
//			Assert.assertNull(referenceModel.getScope());
//			Assert.assertEquals(Foo.class, referenceModel.getServiceClass());
//			Assert.assertEquals("", referenceModel.getTarget());
//
//			referenceModel = referenceIterator.next();
//
//			Assert.assertEquals(Foo.class, referenceModel.getBeanClass());
//			Assert.assertEquals(
//				ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
//			Assert.assertEquals("foo_b", referenceModel.getName());
//			Assert.assertEquals(ReferencePolicy.STATIC, referenceModel.getPolicy());
//			Assert.assertEquals(
//				ReferencePolicyOption.RELUCTANT, referenceModel.getPolicyOption());
//			Assert.assertNull(referenceModel.getScope());
//			Assert.assertEquals(Foo.class, referenceModel.getServiceClass());
//			Assert.assertEquals("", referenceModel.getTarget());
		}
	}

	@Test
	public void test_CtorFooOptional() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorFooOptional.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<ComponentModel> models = sort(beansModel.getComponentModels());

			Assert.assertEquals(1, models.size());

//			Collection<ReferenceModel> references = container.getTrackedReferences();
//
//			Assert.assertEquals(1, references.size());
//
//			Iterator<ReferenceModel> referenceIterator = references.iterator();
//
//			ReferenceModel referenceModel = referenceIterator.next();
//
//			Assert.assertEquals(Foo.class, referenceModel.getBeanClass());
//			Assert.assertEquals(
//				ReferenceCardinality.OPTIONAL, referenceModel.getCardinality());
//			Assert.assertEquals("Foo0", referenceModel.getName());
//			Assert.assertEquals(ReferencePolicy.STATIC, referenceModel.getPolicy());
//			Assert.assertEquals(
//				ReferencePolicyOption.RELUCTANT, referenceModel.getPolicyOption());
//			Assert.assertNull(referenceModel.getScope());
//			Assert.assertEquals(Foo.class, referenceModel.getServiceClass());
//			Assert.assertEquals("", referenceModel.getTarget());
		}
	}

}