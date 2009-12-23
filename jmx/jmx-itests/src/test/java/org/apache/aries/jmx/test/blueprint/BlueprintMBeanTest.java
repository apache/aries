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
import static org.junit.Assert.fail;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;

@RunWith(JUnit4TestRunner.class)
public class BlueprintMBeanTest {
    
    @Inject
    private BundleContext rbc;
    
    MBeanServer mbs;
    ServiceRegistration mbsr;
    
    // will run before each test
    @Configuration
    public static Option[] configuration()
    {      
        return CoreOptions.options(CoreOptions.equinox(), 
                CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").versionAsInProject(), 
                CoreOptions.mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-api").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-service").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.aries.blueprint").artifactId("aries-blueprint").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.aries.blueprint").artifactId("org.apache.aries.blueprint.sample").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.aries.jmx").artifactId("aries-jmx-blueprint-api").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.aries.jmx").artifactId("aries-jmx-blueprint-core").versionAsInProject()
        );
    }  

    @Before
    public void setup() throws Exception {
       System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Before Test");
       
       // Create a MBean Server
       //MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
       mbs = MBeanServerFactory.createMBeanServer();
       
       // Register as a service, so that the blueprint mbean impl can found the server.
       mbsr = rbc.registerService(MBeanServer.class.getCanonicalName(), mbs, null);
       
       // Wait MBeans register in server
       int i=0;
       while (true){
           try {
               mbs.getObjectInstance(new ObjectName(BlueprintStateMBean.OBJECTNAME));
               mbs.getObjectInstance(new ObjectName(BlueprintMetadataMBean.OBJECTNAME));
               System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Found MBeans");
               break;
           }catch(InstanceNotFoundException e){
               if (i==5) throw new Exception(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BlueprintStateMBean & BlueprintMetadataMBean are not found in server");
           }
           i++;
           Thread.sleep(1000);
       }
       
       // Wait enough time for osgi framework and blueprint bundles to be set up
       System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Waiting for bundles to be set up");
       Thread.sleep(10000);
       
    }
    
    @After
    public void teardown(){
        if (mbsr!=null) mbsr.unregister();
    }
        
    @Test
    public void BlueprintSample()throws Exception{
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Start Test Blueprint Sample");
        
        //////////////////////////////
        //Test BlueprintStateMBean
        //////////////////////////////
        
        //find the Blueprint Sample bundle id and the blueprint extender bundle id
        long sampleBundleId = -1;
        long extenderBundleId = -1;     // the blueprint extender bundle "org.apache.geronimo.blueprint.geronimo-blueprint" is also a blueprint bundle.
        for (Bundle bundle : rbc.getBundles()){
            if (bundle.getSymbolicName().equals("org.apache.aries.blueprint.sample")) sampleBundleId = bundle.getBundleId();
            if (bundle.getSymbolicName().equals("org.apache.aries.blueprint")) extenderBundleId = bundle.getBundleId();
        }
        if (-1==sampleBundleId) fail("Blueprint Sample Bundle is not found!");
        if (-1==extenderBundleId) fail("Blueprint Extender Bundle is not found!");
        
        //retrieve the proxy object
        BlueprintStateMBean stateProxy = (BlueprintStateMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, new ObjectName(BlueprintStateMBean.OBJECTNAME), BlueprintStateMBean.class, false);
        
        // test getBlueprintBundleIds
        long[] bpBundleIds = stateProxy.getBlueprintBundleIds();
        assertEquals(2, bpBundleIds.length);
        // test getLastEvent
        BlueprintEventValidator sampleValidator = new BlueprintEventValidator(sampleBundleId, extenderBundleId, 2);
        sampleValidator.validate(stateProxy.getLastEvent(sampleBundleId));
        // test getLastEvents
        TabularData lastEvents = stateProxy.getLastEvents();
        assertEquals(BlueprintStateMBean.OSGI_BLUEPRINT_EVENTS_TYPE,lastEvents.getTabularType());
        sampleValidator.validate(lastEvents.get(new Long[]{sampleBundleId}));
        
        //////////////////////////////
        //Test BlueprintMetadataMBean
        //////////////////////////////
        
        //find the Blueprint Sample bundle's container service id
        Bundle sampleBundle = rbc.getBundle(sampleBundleId);
        String filter = "(&(osgi.blueprint.container.symbolicname=" // no similar one in interfaces
                + sampleBundle.getSymbolicName() + ")(osgi.blueprint.container.version=" + sampleBundle.getVersion() + "))";
        ServiceReference[] serviceReferences = null;
        try {
            serviceReferences = rbc.getServiceReferences(BlueprintContainer.class.getName(), filter);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        long sampleBlueprintContainerServiceId = (Long) serviceReferences[0].getProperty(Constants.SERVICE_ID);
        
        //retrieve the proxy object
        BlueprintMetadataMBean metadataProxy = (BlueprintMetadataMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, new ObjectName(BlueprintMetadataMBean.OBJECTNAME), BlueprintMetadataMBean.class, false);
        
        // test getBlueprintContainerServiceIds
        long[] bpContainerServiceIds = metadataProxy.getBlueprintContainerServiceIds();
        assertEquals(2, bpContainerServiceIds.length);
        
        // test getBlueprintContainerServiceId
        assertEquals(sampleBlueprintContainerServiceId, metadataProxy.getBlueprintContainerServiceId(sampleBundleId));
        
        // test getComponentMetadata
        // bean: foo
        BeanValidator bv_foo = new BeanValidator("org.apache.aries.blueprint.sample.Foo", "init", "destroy");
        
        BeanPropertyValidator bpv_a = new BeanPropertyValidator("a");
        bpv_a.setObjectValueValidator(new ValueValidator("5"));
        
        BeanPropertyValidator bpv_b = new BeanPropertyValidator("b");
        bpv_b.setObjectValueValidator(new ValueValidator("-1"));
        
        BeanPropertyValidator bpv_bar = new BeanPropertyValidator("bar");
        bpv_bar.setObjectValueValidator(new RefValidator("bar"));
        
        BeanPropertyValidator bpv_currency = new BeanPropertyValidator("currency");
        bpv_currency.setObjectValueValidator(new ValueValidator("PLN"));
        
        BeanPropertyValidator bpv_date = new BeanPropertyValidator("date");
        bpv_date.setObjectValueValidator(new ValueValidator("2009.04.17"));
        
        bv_foo.addPropertyValidators(bpv_a, bpv_b, bpv_bar, bpv_currency, bpv_date);
        bv_foo.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, "foo"));
        
        // bean: bar
        BeanPropertyValidator bpv_value = new BeanPropertyValidator("value");
        bpv_value.setObjectValueValidator(new ValueValidator("Hello FooBar"));
        
        BeanPropertyValidator bpv_context = new BeanPropertyValidator("context");
        bpv_context.setObjectValueValidator(new RefValidator("blueprintBundleContext"));
        
        CollectionValidator cv = new CollectionValidator("java.util.List");
        cv.addCollectionValueValidators(new ValueValidator("a list element"), new ValueValidator("5", "java.lang.Integer"));
        BeanPropertyValidator bpv_list = new BeanPropertyValidator("list");
        bpv_list.setObjectValueValidator(cv);
        
        BeanValidator bv_bar = new BeanValidator("org.apache.aries.blueprint.sample.Bar");
        bv_bar.addPropertyValidators(bpv_value, bpv_context, bpv_list);
        bv_bar.validate(metadataProxy.getComponentMetadata(sampleBlueprintContainerServiceId, "bar"));
        
        // service: ref=foo, no componentId set. So using it to test getComponentIdsByType.
        String[] serviceComponentIds = metadataProxy.getComponentIdsByType(sampleBlueprintContainerServiceId, BlueprintMetadataMBean.SERVICE_METADATA);
        assertEquals("There should be only one service component in this sample", 1, serviceComponentIds.length);
        
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
        
        // bean：bindingListener
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
    
        
}
