/**
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
package org.apache.aries.spifly;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import junit.framework.TestCase;

import org.apache.aries.spifly.Activator;
import org.apache.aries.spifly.SPIBundleTracker;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class SPIBundleTrackerTest extends TestCase {
    public void testSPIBundleTrackerClose() {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.replay(bc);
        
        ServiceRegistration sr = EasyMock.createMock(ServiceRegistration.class);
        sr.unregister();
        EasyMock.replay(sr);
        
        SPIBundleTracker sbt = new SPIBundleTracker(bc, new Activator());
        sbt.registrations.add(sr);
        
        sbt.close();        
        EasyMock.verify(sr);
    }
    
    @SuppressWarnings("unchecked")
    public void testAddingBundle() throws Exception {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(EasyMock.createMock(Bundle.class));
        EasyMock.replay(bc);
        
        SPIBundleTracker sbt = new SPIBundleTracker(bc, new Activator());

        URL jarURL = getClass().getResource("TestSPIBundle2_1.0.0.jar");
        Dictionary<String, Object> headers = getManifestHeaders(jarURL);               
        URL url = new URL("jar:" + jarURL + "!/META-INF/services/javax.xml.parsers.DocumentBuilderFactory");
        final ClassLoader mockBundleLoader = new URLClassLoader(new URL[] {jarURL});  
        
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.getSymbolicName()).andReturn("x.y.z").anyTimes();
        EasyMock.expect(b.findEntries("META-INF/services", "*", false))
            .andReturn(Collections.enumeration(Collections.singleton(url)));
        EasyMock.expect(b.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(b.loadClass((String) EasyMock.anyObject())).andAnswer(new IAnswer<Class<?>>() {
            public Class<?> answer() throws Throwable {
                return mockBundleLoader.loadClass((String) EasyMock.getCurrentArguments()[0]);
            }
        });

        BundleContext bc2 = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc2.registerService(EasyMock.eq("javax.xml.parsers.DocumentBuilderFactory"), 
            EasyMock.anyObject(), (Dictionary) EasyMock.anyObject())).andAnswer(new IAnswer<ServiceRegistration>() {
                public ServiceRegistration answer() throws Throwable {
                    Object impl = EasyMock.getCurrentArguments()[1];
                    assertEquals("org.example.test.Test2DomBuilderFactory", impl.getClass().getName());
                    assertNotNull(((Dictionary) EasyMock.getCurrentArguments()[2]).get(SPIBundleTracker.SPI_PROVIDER_URL)); 
                    return EasyMock.createMock(ServiceRegistration.class);
                }
            });
        EasyMock.replay(bc2);

        EasyMock.expect(b.getBundleContext()).andReturn(bc2);
        EasyMock.replay(b);
        
        assertEquals("Precondition failed", 0, sbt.registrations.size());
        sbt.addingBundle(b, null);
        assertEquals(1, sbt.registrations.size());
        
        EasyMock.verify(bc2);
    }

    @SuppressWarnings("unchecked")
    public void testAddingNonMarkedBundle() throws Exception {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(EasyMock.createMock(Bundle.class));
        EasyMock.replay(bc);
        
        SPIBundleTracker sbt = new SPIBundleTracker(bc, new Activator());

        URL jarURL = getClass().getResource("TestSPIBundle_1.0.0.jar");
        Dictionary<String, Object> headers = getManifestHeaders(jarURL);               
        URL url = new URL("jar:" + jarURL + "!/META-INF/services/javax.xml.parsers.DocumentBuilderFactory");
        final ClassLoader mockBundleLoader = new URLClassLoader(new URL[] {jarURL});  
        
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.getSymbolicName()).andReturn("x.y.z").anyTimes();
        EasyMock.expect(b.findEntries("META-INF/services", "*", false))
            .andReturn(Collections.enumeration(Collections.singleton(url)));
        EasyMock.expect(b.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(b.loadClass((String) EasyMock.anyObject())).andAnswer(new IAnswer<Class<?>>() {
            public Class<?> answer() throws Throwable {
                return mockBundleLoader.loadClass((String) EasyMock.getCurrentArguments()[0]);
            }
        });

        BundleContext bc2 = EasyMock.createMock(BundleContext.class);
        // no services are expected to be registered.
        EasyMock.replay(bc2);

        EasyMock.expect(b.getBundleContext()).andReturn(bc2);
        EasyMock.replay(b);
        
        assertEquals("Precondition failed", 0, sbt.registrations.size());
        sbt.addingBundle(b, null);
        assertEquals(0, sbt.registrations.size());
        
        EasyMock.verify(bc2); // verify that bc2.registerService() was never called
    }

    public void testAddingUnrelatedButMarkedBundle() {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(EasyMock.createMock(Bundle.class));
        EasyMock.replay(bc);
        
        SPIBundleTracker sbt = new SPIBundleTracker(bc, new Activator());
        
        Dictionary<String, Object> headers = new Hashtable<String, Object>();
        headers.put(SPIBundleTracker.OPT_IN_HEADER, "somevalue");

        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.getSymbolicName()).andReturn("x.y.z").anyTimes();
        EasyMock.expect(b.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(b.findEntries("META-INF/services", "*", false)).andReturn(null);
        EasyMock.replay(b);

        assertEquals("Precondition failed", 0, sbt.registrations.size());
        sbt.addingBundle(b, null);
        assertEquals(0, sbt.registrations.size());
    }

    public void testRemovedBundle() {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.replay(bc);
        
        SPIBundleTracker sbt = new SPIBundleTracker(bc, new Activator());
        
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.replay(b);
        ServiceReference sref = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sref.getBundle()).andReturn(b);
        EasyMock.replay(sref);
        
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.expect(sreg.getReference()).andReturn(sref);
        sreg.unregister();
        EasyMock.replay(sreg);        
        
        sbt.registrations.add(sreg);
        
        assertEquals("Precondition failed", 1, sbt.registrations.size());
        sbt.removedBundle(b, null, null);
        assertEquals(0, sbt.registrations.size());
        
        EasyMock.verify(sreg);
        EasyMock.verify(sref);
    }
    
    public void testRemoveUnrelatedBundle() {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.replay(bc);
        
        SPIBundleTracker sbt = new SPIBundleTracker(bc, new Activator());
        
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.replay(b);
        ServiceReference sref = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sref.getBundle()).andReturn(b);
        EasyMock.replay(sref);
        
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.expect(sreg.getReference()).andReturn(sref);
        sreg.unregister();
        EasyMock.replay(sreg);        
        
        sbt.registrations.add(sreg);
        
        Bundle b2 = EasyMock.createMock(Bundle.class);
        EasyMock.replay(b2);
        
        assertEquals("Precondition failed", 1, sbt.registrations.size());
        sbt.removedBundle(b2, null, null);
        assertEquals(1, sbt.registrations.size());
    }
    
    public void testAddingSelf() {
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.getSymbolicName()).andReturn("x.y.z");
        EasyMock.replay(b);

        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(b);
        EasyMock.replay(bc);
        
        SPIBundleTracker sbt = new SPIBundleTracker(bc, new Activator());        

        // This should not have any effect. Adding myself as a bundle.
        sbt.addingBundle(b, null);
        
        EasyMock.verify(bc);
    }
    
    private Dictionary<String, Object> getManifestHeaders(URL jarURL) throws IOException {
        JarFile jf = new JarFile(jarURL.getFile());
        try {
            Attributes attrs = jf.getManifest().getMainAttributes();
            Hashtable<String, Object> headers = new Hashtable<String, Object>(); 
            for (Map.Entry<Object, Object> entry : attrs.entrySet()) {
                headers.put(entry.getKey().toString(), entry.getValue());
            }
            return headers;
        } finally {
            jf.close();
        }
    }    
}
