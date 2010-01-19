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
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class SPIBundleTrackerCustomizerTest extends TestCase {    
    public void testAddingBundle() throws Exception {        
        Bundle spiBundle = EasyMock.createMock(Bundle.class);
        EasyMock.replay(spiBundle);
        SPIBundleTrackerCustomizer sbt = new SPIBundleTrackerCustomizer(new Activator(), spiBundle);
        
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
            EasyMock.anyObject(), (Dictionary<?, ?>) EasyMock.anyObject())).andAnswer(new IAnswer<ServiceRegistration>() {
                public ServiceRegistration answer() throws Throwable {
                    Object impl = EasyMock.getCurrentArguments()[1];
                    assertEquals("org.example.test.Test2DomBuilderFactory", impl.getClass().getName());
                    assertNotNull(((Dictionary<?, ?>) EasyMock.getCurrentArguments()[2])
                        .get(SPIBundleTrackerCustomizer.SPI_PROVIDER_URL)); 
                    return EasyMock.createMock(ServiceRegistration.class);
                }
            });
        EasyMock.replay(bc2);

        EasyMock.expect(b.getBundleContext()).andReturn(bc2);
        EasyMock.replay(b);

        assertEquals(1, ((List<?>) sbt.addingBundle(b, null)).size());
        
        EasyMock.verify(bc2);
        EasyMock.verify(b);
        EasyMock.verify(spiBundle);
    }
    
    public void testAddingNonMarkedBundle() throws Exception {
        Bundle spiBundle = EasyMock.createMock(Bundle.class);
        EasyMock.replay(spiBundle);
        SPIBundleTrackerCustomizer sbt = new SPIBundleTrackerCustomizer(new Activator(), spiBundle);

        URL jarURL = getClass().getResource("TestSPIBundle_1.0.0.jar");
        Dictionary<String, Object> headers = getManifestHeaders(jarURL);               
        URL url = new URL("jar:" + jarURL + "!/META-INF/services/javax.xml.parsers.DocumentBuilderFactory");
        final ClassLoader mockBundleLoader = new URLClassLoader(new URL[] {jarURL});  
        
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.getSymbolicName()).andReturn("x.y.z").anyTimes();
        EasyMock.expect(b.findEntries("META-INF/services", "*", false))
            .andReturn(Collections.enumeration(Collections.singleton(url))).anyTimes();
        EasyMock.expect(b.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(b.loadClass((String) EasyMock.anyObject())).andAnswer(new IAnswer<Class<?>>() {
            public Class<?> answer() throws Throwable {
                return mockBundleLoader.loadClass((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();

        BundleContext bc2 = EasyMock.createMock(BundleContext.class);
        // no services are expected to be registered.
        EasyMock.replay(bc2);

        EasyMock.expect(b.getBundleContext()).andReturn(bc2).anyTimes();
        EasyMock.replay(b);
        
        assertNull(sbt.addingBundle(b, null));
        
        EasyMock.verify(bc2); // verify that bc2.registerService() was never called
        EasyMock.verify(b);
        EasyMock.verify(spiBundle);        
    }

    public void testAddingUnrelatedButMarkedBundle() {
        Bundle spiBundle = EasyMock.createMock(Bundle.class);
        EasyMock.replay(spiBundle);
        SPIBundleTrackerCustomizer sbt = new SPIBundleTrackerCustomizer(new Activator(), spiBundle);
        
        Dictionary<String, Object> headers = new Hashtable<String, Object>();
        headers.put(SPIBundleTrackerCustomizer.OPT_IN_HEADER, "somevalue");

        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.getSymbolicName()).andReturn("x.y.z").anyTimes();
        EasyMock.expect(b.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(b.findEntries("META-INF/services", "*", false)).andReturn(null);
        EasyMock.replay(b);

        assertNull(sbt.addingBundle(b, null));
        EasyMock.verify(b);
        EasyMock.verify(spiBundle);        
    }

    public void testAddingSelf() {
        Bundle spiBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(spiBundle.getSymbolicName()).andReturn("a.b.c").anyTimes();
        EasyMock.replay(spiBundle);
        SPIBundleTrackerCustomizer sbt = new SPIBundleTrackerCustomizer(new Activator(), spiBundle);

        assertNull(sbt.addingBundle(spiBundle, null));
        EasyMock.verify(spiBundle);
    }
    
    public void testRemovedBundle() {
        Bundle spiBundle = EasyMock.createMock(Bundle.class);
        EasyMock.replay(spiBundle);
        SPIBundleTrackerCustomizer sbt = new SPIBundleTrackerCustomizer(new Activator(), spiBundle);

        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.replay(b);
        
        ServiceRegistration sr1 = EasyMock.createMock(ServiceRegistration.class);
        sr1.unregister();
        EasyMock.replay(sr1);
        ServiceRegistration sr2 = EasyMock.createMock(ServiceRegistration.class);
        sr2.unregister();
        EasyMock.replay(sr2);
        List<ServiceRegistration> regs = Arrays.asList(sr1, sr2);
        sbt.removedBundle(b, null, regs);
        
        EasyMock.verify(sr1);
        EasyMock.verify(sr2);
    }

    public void testRemovedUnrelatedBundle() {
        Bundle spiBundle = EasyMock.createMock(Bundle.class);
        EasyMock.replay(spiBundle);
        SPIBundleTrackerCustomizer sbt = new SPIBundleTrackerCustomizer(new Activator(), spiBundle);

        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.replay(b);
        
        sbt.removedBundle(b, null, b);
        EasyMock.verify(b);
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
