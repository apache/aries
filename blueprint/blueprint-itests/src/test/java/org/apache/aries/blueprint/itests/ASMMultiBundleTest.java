/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.ArrayList;
import java.util.List;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.testbundlea.NSHandlerOne;
import org.apache.aries.blueprint.testbundlea.NSHandlerTwo;
import org.apache.aries.blueprint.testbundlea.ProcessableBean;
import org.apache.aries.blueprint.testbundlea.ProcessableBean.Phase;
import org.apache.aries.blueprint.testbundleb.OtherBean;
import org.apache.aries.blueprint.testbundleb.TestBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintContainer;

@RunWith(PaxExam.class)
public class ASMMultiBundleTest extends AbstractBlueprintIntegrationTest {

    private void checkInterceptorLog(String []expected, List<String> log){
        assertNotNull("interceptor log should not be null",log);
        System.out.println("Log:");
        for(String entry: log){
            System.out.println(""+entry);
        }
        assertEquals("interceptor log size does not match expected size",expected.length,log.size());
        
        List<String> extra=new ArrayList<String>();
        boolean[] found = new boolean[expected.length];
        for(String s : log){
           boolean used=false;
           for(int i=0; i<expected.length; i++){
               if(s.startsWith(expected[i])){
                   found[i]=true;
                   used=true;
               }
           }
           if(!used){
               extra.add(s);
           }
        }
        if(extra.size()!=0){
            String extraFormatted="{";
            for(String e:extra){
                extraFormatted+=e+" ";
            }
            extraFormatted+="}";
            fail("surplus interceptor invocations present in invocation log "+extraFormatted);
        }        
        for(int i=0; i<found.length; i++){
            assertTrue("interceptor invocation "+expected[i]+" not found",found[i]);
        }
    }

    @Test
    public void multiBundleTest() throws Exception {
        
        //bundlea provides the ns handlers, bean processors, interceptors etc for this test.
        Bundle bundlea = context().getBundleByName("org.apache.aries.blueprint.testbundlea");
        assertNotNull(bundlea);
        bundlea.start();
        
        //bundleb makes use of the extensions provided by bundlea
        Bundle bundleb = context().getBundleByName("org.apache.aries.blueprint.testbundleb");
        assertNotNull(bundleb);
        bundleb.start();
        
        //bundleb's container will hold the beans we need to query to check the function
        //provided by bundlea functioned as expected
        BlueprintContainer beanContainer = 
            Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.testbundleb");
        assertNotNull(beanContainer);

        //TestBeanA should have the values below, no interference should be present from other sources.
        Object obj1 = beanContainer.getComponentInstance("TestBeanA");
        assertTrue(obj1 instanceof TestBean);
        TestBean testBeanA = (TestBean)obj1;
        org.junit.Assert.assertEquals("RED", testBeanA.getRed());
        org.junit.Assert.assertEquals("GREEN", testBeanA.getGreen());
        org.junit.Assert.assertEquals("BLUE", testBeanA.getBlue());

        //TestBeanB tests that a custom ns handler is able to inject custom components to the blueprint, 
        //and modify existing components, and use injected components as modifications. 
        Object obj2 = beanContainer.getComponentInstance("TestBeanB");
        assertTrue(obj2 instanceof TestBean);
        TestBean testBeanB = (TestBean)obj2;
        //value should be set in via the added passthroughmetadata via the nshandler.
        org.junit.Assert.assertEquals("ONE_VALUE", testBeanB.getRed());
        org.junit.Assert.assertEquals("GREEN", testBeanB.getGreen());
        org.junit.Assert.assertEquals("BLUE", testBeanB.getBlue());        
        
        //TestBeanC tests that custom ns handlers can add interceptors to beans.
        Object obj3 = beanContainer.getComponentInstance("TestBeanC");
        assertTrue(obj3 instanceof TestBean);
        TestBean testBeanC = (TestBean)obj3;
       
        //handlers are in bundlea, with its own container.
        BlueprintContainer handlerContainer = 
            Helper.getBlueprintContainerForBundle( context(), "org.apache.aries.blueprint.testbundlea");
        assertNotNull(handlerContainer);
        
        Object ns1 = handlerContainer.getComponentInstance("NSHandlerOne");
        assertTrue(ns1 instanceof NSHandlerOne);
        
        Object ns2 = handlerContainer.getComponentInstance("NSHandlerTwo");
        assertTrue(ns2 instanceof NSHandlerTwo);
        NSHandlerTwo nstwo = (NSHandlerTwo)ns2;
        
        //now we have a handle to the nshandler2, we can query what it 'saw', and ensure
        //that the interceptors are functioning as expected.
        List<String> log = nstwo.getLog();
        
        //TestBeanC has the interceptor configured, and is injected to OtherBeanA & OtherBeanB
        //which then uses the injected bean during their init method call, to invoke a method
        checkInterceptorLog(new String[] {
        "PRECALL:TestBeanC:methodToInvoke:[RED]:",
        "POSTCALL[true]:TestBeanC:methodToInvoke:[RED]:",
        "PRECALL:TestBeanC:methodToInvoke:[BLUE]:",
        "POSTCALL[false]:TestBeanC:methodToInvoke:[BLUE]:"
         }, log);
        
        //invoking GREEN is hardwired to cause an exception response, we do this 
        //from here to ensure the exception occurs and is visible as expected
        RuntimeException re=null;
        try{
          testBeanC.methodToInvoke("GREEN");
        }catch(RuntimeException e){
            re=e;
        }
        assertNotNull("invocation of Green did not cause an exception as expected",re);
        
        //Exception responses should be intercepted too, test for the POSTCALLWITHEXCEPTION log entry.
        log = nstwo.getLog();
        checkInterceptorLog(new String[] {
                "PRECALL:TestBeanC:methodToInvoke:[RED]:",
                "POSTCALL[true]:TestBeanC:methodToInvoke:[RED]:",
                "PRECALL:TestBeanC:methodToInvoke:[BLUE]:",
                "POSTCALL[false]:TestBeanC:methodToInvoke:[BLUE]:",
                "PRECALL:TestBeanC:methodToInvoke:[GREEN]:",
                "POSTCALLEXCEPTION[java.lang.RuntimeException: MATCHED ON GREEN (GREEN)]:TestBeanC:methodToInvoke:[GREEN]:"
                 }, log);
        
        //ProcessedBean is a test to ensure that BeanProcessors are called.. 
        //The test has the BeanProcessor look for ProcessableBeans, and log itself with them
        Object obj4 = beanContainer.getComponentInstance("ProcessedBean");
        assertTrue(obj4 instanceof ProcessableBean);
        ProcessableBean pb = (ProcessableBean)obj4;
        
        //Note, the BeanProcessor exists in the same container as the beans it processes!! 
        Object bp = beanContainer.getComponentInstance("http://ns.handler.three/BeanProcessor");
        assertNotNull(bp);
        assertTrue(bp instanceof BeanProcessor);
        assertEquals(1,pb.getProcessedBy().size());
        //check we were invoked..
        assertEquals(pb.getProcessedBy().get(0),bp);
        //check invocation for each phase.
        assertEquals(pb.getProcessedBy(Phase.BEFORE_INIT).get(0),bp);
        assertEquals(pb.getProcessedBy(Phase.AFTER_INIT).get(0),bp);
        //destroy invocation will only occur at tear down.. TODO, how to test after teardown.
        //assertEquals(pb.getProcessedBy(Phase.BEFORE_DESTROY).get(0),bp);
        //assertEquals(pb.getProcessedBy(Phase.AFTER_DESTROY).get(0),bp);
        
        
        Object objOther = beanContainer.getComponentInstance("PlaceHolderTestBean");
        assertTrue(objOther instanceof OtherBean);
        assertEquals("test1value", ((OtherBean)objOther).getTestValue());
    }

    @Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles(),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.testbundlea").noStart(),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.testbundleb").noStart()
        };
    } 
}
