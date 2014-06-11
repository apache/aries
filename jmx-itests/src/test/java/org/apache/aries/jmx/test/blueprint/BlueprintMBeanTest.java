/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jmx.test.blueprint;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.Arrays;

import javax.inject.Inject;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;
import org.apache.aries.jmx.blueprint.BlueprintStateMBean;
import org.apache.aries.jmx.test.blueprint.framework.BeanPropertyValidator;
import org.apache.aries.jmx.test.blueprint.framework.BeanValidator;
import org.apache.aries.jmx.test.blueprint.framework.BlueprintEventValidator;
import org.apache.aries.jmx.test.blueprint.framework.CollectionValidator;
import org.apache.aries.jmx.test.blueprint.framework.MapEntryValidator;
import org.apache.aries.jmx.test.blueprint.framework.RefValidator;
import org.apache.aries.jmx.test.blueprint.framework.ReferenceListValidator;
import org.apache.aries.jmx.test.blueprint.framework.ReferenceListenerValidator;
import org.apache.aries.jmx.test.blueprint.framework.ReferenceValidator;
import org.apache.aries.jmx.test.blueprint.framework.RegistrationListenerValidator;
import org.apache.aries.jmx.test.blueprint.framework.ServiceValidator;
import org.apache.aries.jmx.test.blueprint.framework.ValueValidator;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class BlueprintMBeanTest extends AbstractIntegrationTest {
	@Inject
	@Filter("(osgi.blueprint.container.symbolicname=org.apache.aries.blueprint)")
	BlueprintContainer blueprintExtender;
	
	@Inject
	@Filter("(osgi.blueprint.container.symbolicname=org.apache.aries.blueprint.sample)")
	BlueprintContainer blueprintSample;

	private Bundle extender;
	private Bundle sample;

    @Configuration
    public Option[] configuration() {
        return CoreOptions.options(
        		jmxRuntime(),
        		blueprint(),
        		mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.blueprint").versionAsInProject(),
        		mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample").versionAsInProject()
        );
    }

	protected Option blueprint() {
		return composite(
				mavenBundle("org.ow2.asm", "asm-debug-all").versionAsInProject(),
				mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
				mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint").versionAsInProject(),
				mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.jexl.evaluator").versionAsInProject(),
				mavenBundle("org.apache.commons", "commons-jexl").versionAsInProject()
				);
	}
	
	@Before
	public void setup() {
		extender = getBundleByName("org.apache.aries.blueprint");
		sample = getBundleByName("org.apache.aries.blueprint.sample");
	}

    @Test
    public void testBlueprintStateMBean() throws Exception {
        BlueprintStateMBean stateProxy = getMBean(BlueprintStateMBean.OBJECTNAME, BlueprintStateMBean.class);

        // test getBlueprintBundleIds
        long[] bpBundleIds = stateProxy.getBlueprintBundleIds();
        assertEquals("The blueprint bundle ids are: " + Arrays.toString(bpBundleIds), 3, bpBundleIds.length);
        // test getLastEvent
        BlueprintEventValidator sampleValidator = new BlueprintEventValidator(sample.getBundleId(), extender.getBundleId(), 2);
        sampleValidator.validate(stateProxy.getLastEvent(sample.getBundleId()));
        // test getLastEvents
        TabularData lastEvents = stateProxy.getLastEvents();
        assertEquals(BlueprintStateMBean.OSGI_BLUEPRINT_EVENTS_TYPE,lastEvents.getTabularType());
        sampleValidator.validate(lastEvents.get(new Long[]{sample.getBundleId()}));
    }
    
    @Test
    public void testBlueprintMetaDataMBean() throws Exception {
        //find the Blueprint Sample bundle's container service id
        String filter = "(&(osgi.blueprint.container.symbolicname=" // no similar one in interfaces
                + sample.getSymbolicName() + ")(osgi.blueprint.container.version=" + sample.getVersion() + "))";
        ServiceReference[] serviceReferences = null;
        try {
            serviceReferences = bundleContext.getServiceReferences(BlueprintContainer.class.getName(), filter);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        long sampleBlueprintContainerServiceId = (Long) serviceReferences[0].getProperty(Constants.SERVICE_ID);

        //retrieve the proxy object
        BlueprintMetadataMBean metadataProxy = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, new ObjectName(BlueprintMetadataMBean.OBJECTNAME), BlueprintMetadataMBean.class, false);

        // test getBlueprintContainerServiceIds
        long[] bpContainerServiceIds = metadataProxy.getBlueprintContainerServiceIds();
        assertEquals(3, bpContainerServiceIds.length);

        // test getBlueprintContainerServiceId
        assertEquals(sampleBlueprintContainerServiceId, metadataProxy.getBlueprintContainerServiceId(sample.getBundleId()));

        // test getComponentMetadata
        // bean: foo
        BeanValidator bv_foo = new BeanValidator("org.apache.aries.blueprint.sample.Foo", "init", "destroy");

        BeanPropertyValidator bpv_a = property("a", "5");
        BeanPropertyValidator bpv_b = property("b", "-1");
        BeanPropertyValidator bpv_bar = new BeanPropertyValidator("bar");
        bpv_bar.setObjectValueValidator(new RefValidator("bar"));
        BeanPropertyValidator bpv_currency = property("currency", "PLN");
        BeanPropertyValidator bpv_date = property("date", "2009.04.17");

        bv_foo.addPropertyValidators(bpv_a, bpv_b, bpv_bar, bpv_currency, bpv_date);
        bv_foo.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, "foo"));

        // bean: bar
        BeanPropertyValidator bpv_value = property("value", "Hello FooBar");
        BeanPropertyValidator bpv_context = new BeanPropertyValidator("context");
        bpv_context.setObjectValueValidator(new RefValidator("blueprintBundleContext"));

        CollectionValidator cv = new CollectionValidator("java.util.List");
        cv.addCollectionValueValidators(
        		new ValueValidator("a list element"), 
        		new ValueValidator("5", "java.lang.Integer"));
        BeanPropertyValidator bpv_list = new BeanPropertyValidator("list");
        bpv_list.setObjectValueValidator(cv);

        BeanValidator bv_bar = new BeanValidator("org.apache.aries.blueprint.sample.Bar");
        bv_bar.addPropertyValidators(bpv_value, bpv_context, bpv_list);
        bv_bar.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, "bar"));

        // service: ref=foo, no componentId set. So using it to test getComponentIdsByType.
        String[] serviceComponentIds = metadataProxy.getComponentIdsByType(sampleBlueprintContainerServiceId, BlueprintMetadataMBean.SERVICE_METADATA);
        assertEquals("There should be two service components in this sample", 2, serviceComponentIds.length);

        MapEntryValidator mev = new MapEntryValidator();
        mev.setKeyValueValidator(new ValueValidator("key"), new ValueValidator("value"));

        RegistrationListenerValidator rglrv = new RegistrationListenerValidator("serviceRegistered", "serviceUnregistered");
        rglrv.setListenerComponentValidator(new RefValidator("fooRegistrationListener"));

        ServiceValidator sv = new ServiceValidator(4);
        sv.setServiceComponentValidator(new RefValidator("foo"));
        sv.addMapEntryValidator(mev);
        sv.addRegistrationListenerValidator(rglrv);
        sv.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, serviceComponentIds[0]));

        // bean: fooRegistrationListener
        BeanValidator bv_fooRegistrationListener = new BeanValidator("org.apache.aries.blueprint.sample.FooRegistrationListener");
        bv_fooRegistrationListener.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, "fooRegistrationListener"));

        // reference: ref2
        ReferenceListenerValidator rlrv_1 = new ReferenceListenerValidator("bind", "unbind");
        rlrv_1.setListenerComponentValidator(new RefValidator("bindingListener"));

        ReferenceValidator rv = new ReferenceValidator("org.apache.aries.blueprint.sample.InterfaceA", 100);
        rv.addReferenceListenerValidator(rlrv_1);
        rv.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, "ref2"));

        // bean: bindingListener
        BeanValidator bv_bindingListener = new BeanValidator("org.apache.aries.blueprint.sample.BindingListener");
        bv_bindingListener.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, "bindingListener"));

        // reference-list: ref-list
        ReferenceListenerValidator rlrv_2 = new ReferenceListenerValidator("bind", "unbind");
        rlrv_2.setListenerComponentValidator(new RefValidator("listBindingListener"));

        ReferenceListValidator rlv_ref_list = new ReferenceListValidator("org.apache.aries.blueprint.sample.InterfaceA");
        rlv_ref_list.addReferenceListenerValidator(rlrv_2);
        rlv_ref_list.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, "ref-list"));

        // bean: listBindingListener
        BeanValidator bv_listBindingListener = new BeanValidator("org.apache.aries.blueprint.sample.BindingListener");
        bv_listBindingListener.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, "listBindingListener"));

        // bean: circularReference
        ReferenceListenerValidator rlrv_3 = new ReferenceListenerValidator("bind", "unbind");
        rlrv_3.setListenerComponentValidator(new RefValidator("circularReference"));

        ReferenceListValidator rlv_2 = new ReferenceListValidator("org.apache.aries.blueprint.sample.InterfaceA", 2);
        rlv_2.addReferenceListenerValidator(rlrv_3);

        BeanPropertyValidator bpv_list_2 = new BeanPropertyValidator("list");
        bpv_list_2.setObjectValueValidator(rlv_2);

        BeanValidator bv_circularReference = new BeanValidator("org.apache.aries.blueprint.sample.BindingListener", "init");
        bv_circularReference.addPropertyValidators(bpv_list_2);
        bv_circularReference.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, "circularReference"));
    }

	private BeanPropertyValidator property(String name, String expectedValue) {
		BeanPropertyValidator val = new BeanPropertyValidator(name);
        val.setObjectValueValidator(new ValueValidator(expectedValue));
		return val;
	}

}
