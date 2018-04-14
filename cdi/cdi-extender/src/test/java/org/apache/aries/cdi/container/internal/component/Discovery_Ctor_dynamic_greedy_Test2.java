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

import static org.apache.aries.cdi.container.test.TestUtil.sort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.v2.component.Component;
import org.apache.aries.cdi.container.test.MockCdiContainerAndComponents;
import org.apache.aries.cdi.container.test.beans.Bar;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.apache.aries.cdi.container.test.beans.ctordynamicgreedy.CtorCollectionFoo;
import org.apache.aries.cdi.container.test.beans.ctordynamicgreedy.CtorFoo;
import org.apache.aries.cdi.container.test.beans.ctordynamicgreedy.CtorFooBar;
import org.apache.aries.cdi.container.test.beans.ctordynamicgreedy.CtorFooFoo;
import org.apache.aries.cdi.container.test.beans.ctordynamicgreedy.CtorFooFooNamed;
import org.apache.aries.cdi.container.test.beans.ctordynamicgreedy.CtorFooOptional;
import org.apache.aries.cdi.container.test.beans.ctordynamicgreedy.CtorListFoo;
import org.junit.Test;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class Discovery_Ctor_dynamic_greedy_Test2 {

	@Test
	public void test_CtorCollectionFoo() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorCollectionFoo.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<OSGiBean> beans = sort(beansModel.getOSGiBeans());

			assertEquals(1, beans.size());

			OSGiBean bean = beans.iterator().next();

			Component component = bean.getComponent();

			assertNotNull(component);

			ComponentTemplateDTO template = component.getTemplate();

			assertNotNull(template);
			assertNotNull(template.references);

			assertEquals(1, template.references.size());

			ReferenceTemplateDTO referenceTemplateDTO = template.references.get(0);

			assertEquals(MaximumCardinality.MANY, referenceTemplateDTO.maximumCardinality);
			assertEquals(0, referenceTemplateDTO.minimumCardinality);
			assertEquals(CtorCollectionFoo.class.getName() + ".new0", referenceTemplateDTO.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, referenceTemplateDTO.policy);
			assertEquals(
				ReferenceTemplateDTO.PolicyOption.GREEDY, referenceTemplateDTO.policyOption);
			assertEquals(Foo.class.getName(), referenceTemplateDTO.serviceType);
			assertEquals("", referenceTemplateDTO.targetFilter);
		}
	}

	@Test
	public void test_CtorFooFoo() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorFooFoo.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<OSGiBean> beans = sort(beansModel.getOSGiBeans());

			assertEquals(1, beans.size());

			OSGiBean bean = beans.iterator().next();

			Component component = bean.getComponent();

			assertNotNull(component);

			ComponentTemplateDTO template = component.getTemplate();

			assertNotNull(template);
			assertNotNull(template.references);

			assertEquals(2, template.references.size());

			ReferenceTemplateDTO referenceTemplateDTO = template.references.get(0);

			assertEquals(MaximumCardinality.ONE, referenceTemplateDTO.maximumCardinality);
			assertEquals(1, referenceTemplateDTO.minimumCardinality);
			assertEquals(CtorFooFoo.class.getName() + ".new0", referenceTemplateDTO.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, referenceTemplateDTO.policy);
			assertEquals(
				ReferenceTemplateDTO.PolicyOption.GREEDY, referenceTemplateDTO.policyOption);
			assertEquals(Foo.class.getName(), referenceTemplateDTO.serviceType);
			assertEquals("", referenceTemplateDTO.targetFilter);

			referenceTemplateDTO = template.references.get(1);

			assertEquals(MaximumCardinality.ONE, referenceTemplateDTO.maximumCardinality);
			assertEquals(1, referenceTemplateDTO.minimumCardinality);
			assertEquals(CtorFooFoo.class.getName() + ".new1", referenceTemplateDTO.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, referenceTemplateDTO.policy);
			assertEquals(
				ReferenceTemplateDTO.PolicyOption.GREEDY, referenceTemplateDTO.policyOption);
			assertEquals(Foo.class.getName(), referenceTemplateDTO.serviceType);
			assertEquals("", referenceTemplateDTO.targetFilter);
		}
	}

	@Test
	public void test_CtorFoo() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorFoo.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<OSGiBean> beans = sort(beansModel.getOSGiBeans());

			assertEquals(1, beans.size());

			OSGiBean bean = beans.iterator().next();

			Component component = bean.getComponent();

			assertNotNull(component);

			ComponentTemplateDTO template = component.getTemplate();

			assertNotNull(template);
			assertNotNull(template.references);

			assertEquals(1, template.references.size());

			ReferenceTemplateDTO referenceTemplateDTO = template.references.get(0);

			assertEquals(MaximumCardinality.ONE, referenceTemplateDTO.maximumCardinality);
			assertEquals(1, referenceTemplateDTO.minimumCardinality);
			assertEquals(CtorFoo.class.getName() + ".new0", referenceTemplateDTO.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, referenceTemplateDTO.policy);
			assertEquals(
				ReferenceTemplateDTO.PolicyOption.GREEDY, referenceTemplateDTO.policyOption);
			assertEquals(Foo.class.getName(), referenceTemplateDTO.serviceType);
			assertEquals("", referenceTemplateDTO.targetFilter);
		}
	}

	@Test
	public void test_CtorListFoo() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorListFoo.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<OSGiBean> beans = sort(beansModel.getOSGiBeans());

			assertEquals(1, beans.size());

			OSGiBean bean = beans.iterator().next();

			Component component = bean.getComponent();

			assertNotNull(component);

			ComponentTemplateDTO template = component.getTemplate();

			assertNotNull(template);
			assertNotNull(template.references);

			assertEquals(1, template.references.size());

			ReferenceTemplateDTO referenceTemplateDTO = template.references.get(0);

			assertEquals(MaximumCardinality.MANY, referenceTemplateDTO.maximumCardinality);
			assertEquals(0, referenceTemplateDTO.minimumCardinality);
			assertEquals(CtorListFoo.class.getName() + ".new0", referenceTemplateDTO.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, referenceTemplateDTO.policy);
			assertEquals(
				ReferenceTemplateDTO.PolicyOption.GREEDY, referenceTemplateDTO.policyOption);
			assertEquals(Foo.class.getName(), referenceTemplateDTO.serviceType);
			assertEquals("", referenceTemplateDTO.targetFilter);
		}
	}

	@Test
	public void test_CtorFooBar() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorFooBar.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<OSGiBean> beans = sort(beansModel.getOSGiBeans());

			assertEquals(1, beans.size());

			OSGiBean bean = beans.iterator().next();

			Component component = bean.getComponent();

			assertNotNull(component);

			ComponentTemplateDTO template = component.getTemplate();

			assertNotNull(template);
			assertNotNull(template.references);

			assertEquals(2, template.references.size());

			ReferenceTemplateDTO referenceTemplateDTO = template.references.get(0);

			assertEquals(MaximumCardinality.ONE, referenceTemplateDTO.maximumCardinality);
			assertEquals(1, referenceTemplateDTO.minimumCardinality);
			assertEquals(CtorFooBar.class.getName() + ".new0", referenceTemplateDTO.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, referenceTemplateDTO.policy);
			assertEquals(
				ReferenceTemplateDTO.PolicyOption.GREEDY, referenceTemplateDTO.policyOption);
			assertEquals(Foo.class.getName(), referenceTemplateDTO.serviceType);
			assertEquals("", referenceTemplateDTO.targetFilter);

			referenceTemplateDTO = template.references.get(1);

			assertEquals(MaximumCardinality.ONE, referenceTemplateDTO.maximumCardinality);
			assertEquals(1, referenceTemplateDTO.minimumCardinality);
			assertEquals(CtorFooBar.class.getName() + ".new1", referenceTemplateDTO.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, referenceTemplateDTO.policy);
			assertEquals(
				ReferenceTemplateDTO.PolicyOption.GREEDY, referenceTemplateDTO.policyOption);
			assertEquals(Bar.class.getName(), referenceTemplateDTO.serviceType);
			assertEquals("", referenceTemplateDTO.targetFilter);
		}
	}

	@Test
	public void test_CtorFooFooNamed() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorFooFooNamed.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<OSGiBean> beans = sort(beansModel.getOSGiBeans());

			assertEquals(1, beans.size());

			OSGiBean bean = beans.iterator().next();

			Component component = bean.getComponent();

			assertNotNull(component);

			ComponentTemplateDTO template = component.getTemplate();

			assertNotNull(template);
			assertNotNull(template.references);

			assertEquals(2, template.references.size());

			ReferenceTemplateDTO referenceTemplateDTO = template.references.get(0);

			assertEquals(MaximumCardinality.ONE, referenceTemplateDTO.maximumCardinality);
			assertEquals(1, referenceTemplateDTO.minimumCardinality);
			assertEquals("foo_a", referenceTemplateDTO.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, referenceTemplateDTO.policy);
			assertEquals(
				ReferenceTemplateDTO.PolicyOption.GREEDY, referenceTemplateDTO.policyOption);
			assertEquals(Foo.class.getName(), referenceTemplateDTO.serviceType);
			assertEquals("", referenceTemplateDTO.targetFilter);

			referenceTemplateDTO = template.references.get(1);

			assertEquals(MaximumCardinality.ONE, referenceTemplateDTO.maximumCardinality);
			assertEquals(1, referenceTemplateDTO.minimumCardinality);
			assertEquals("foo_b", referenceTemplateDTO.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, referenceTemplateDTO.policy);
			assertEquals(
				ReferenceTemplateDTO.PolicyOption.GREEDY, referenceTemplateDTO.policyOption);
			assertEquals(Foo.class.getName(), referenceTemplateDTO.serviceType);
			assertEquals("", referenceTemplateDTO.targetFilter);
		}
	}

	@Test
	public void test_CtorFooOptional() throws Exception {
		try (MockCdiContainerAndComponents container = new MockCdiContainerAndComponents(
				"test", CtorFooOptional.class.getName())) {

			BeansModel beansModel = container.containerState().beansModel();

			Collection<OSGiBean> beans = sort(beansModel.getOSGiBeans());

			assertEquals(1, beans.size());

			OSGiBean bean = beans.iterator().next();

			Component component = bean.getComponent();

			assertNotNull(component);

			ComponentTemplateDTO template = component.getTemplate();

			assertNotNull(template);
			assertNotNull(template.references);

			assertEquals(1, template.references.size());

			ReferenceTemplateDTO referenceTemplateDTO = template.references.get(0);

			assertEquals(MaximumCardinality.ONE, referenceTemplateDTO.maximumCardinality);
			assertEquals(0, referenceTemplateDTO.minimumCardinality);
			assertEquals(CtorFooOptional.class.getName() + ".new0", referenceTemplateDTO.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, referenceTemplateDTO.policy);
			assertEquals(
				ReferenceTemplateDTO.PolicyOption.GREEDY, referenceTemplateDTO.policyOption);
			assertEquals(Foo.class.getName(), referenceTemplateDTO.serviceType);
			assertEquals("", referenceTemplateDTO.targetFilter);
		}
	}

}