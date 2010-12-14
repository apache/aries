package org.apache.aries.spifly;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.aries.spifly.api.SpiFlyConstants;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

public class ClientWeavingHookTest {
    @Before
    public void setUp() {
        Activator.activator = new Activator();
    }
        
    @Test
    public void testClientWeavingHook() throws Exception {
        // Set up the classloader that will be used by the ASM-generated code as the TCCL. 
        // It can load a META-INF/services file
        ClassLoader cl = new TestImplClassLoader("impl1", "META-INF/services/org.apache.aries.mytest.MySPI");
        
        // The BundleWiring API is used on the bundle by the generated code to obtain its classloader
        BundleWiring bw = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(bw.getClassLoader()).andReturn(cl);
        EasyMock.replay(bw);
        
        // Create a mock object for the client bundle which holds the code that uses ServiceLoader.load().
        Bundle testBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(testBundle.getSymbolicName()).andReturn("mytestbundle");
        EasyMock.expect(testBundle.getVersion()).andReturn(Version.parseVersion("1.2.3"));
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, "true");
        EasyMock.expect(testBundle.getHeaders()).andReturn(headers);
        EasyMock.expect(testBundle.adapt(BundleWiring.class)).andReturn(bw);
        EasyMock.replay(testBundle);
        
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(testBundle);
        EasyMock.replay(bc);
        
        WeavingHook wh = new ClientWeavingHook(bc);
        
        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        Assert.assertNotNull("precondition", clsUrl);
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.TestClient", testBundle);
        Assert.assertEquals("Precondition", 0, wc.getDynamicImports().size());
        wh.weave(wc);
        Assert.assertEquals(1, wc.getDynamicImports().size());
        String di1 = "org.apache.aries.spifly;bundle-symbolic-name=mytestbundle;bundle-version=1.2.3";
        String di2 = "org.apache.aries.spifly;bundle-version=1.2.3;bundle-symbolic-name=mytestbundle";
        String di = wc.getDynamicImports().get(0);
        Assert.assertTrue("Weaving should have added a dynamic import", di1.equals(di) || di2.equals(di));        
        
        // ok the weaving is done, now prepare the registry for the call
        Activator.activator.registerSPIProviderBundle("org.apache.aries.mytest.MySPI", testBundle);
        
        // Invoke the woven class and check that it propertly sets the TCCL so that the 
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl1 is visible.
        Class<?> cls = wc.getDefinedClass();
        Object inst = cls.newInstance();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(inst, "hello");
        Assert.assertEquals("olleh", result);
    }
        
    private class TestImplClassLoader extends URLClassLoader {
        private final List<String> resources;
        private final String prefix;
        
        public TestImplClassLoader(String subdir, String ... resources) {
            super(new URL [] {}, TestImplClassLoader.class.getClassLoader());
            
            this.prefix = TestImplClassLoader.class.getPackage().getName().replace('.', '/') + "/" + subdir + "/";
            this.resources = Arrays.asList(resources);
        }

        @Override
        public URL findResource(String name) {
            if (resources.contains(name)) {
                return getClass().getClassLoader().getResource(prefix + name);
            } else {
                return super.findResource(name);
            }
        }

        @Override
        public Enumeration<URL> findResources(String name) throws IOException {
            if (resources.contains(name)) {
                return getClass().getClassLoader().getResources(prefix + name);
            } else {
                return super.findResources(name);
            }
        }
    }    

    private static class MyWovenClass implements WovenClass {
        byte [] bytes;
        final String className;
        final Bundle bundleContainingOriginalClass;
        List<String> dynamicImports = new ArrayList<String>();
        boolean weavingComplete = false;
        
        private MyWovenClass(URL clazz, String name, Bundle bundle) throws Exception {
            bytes = Streams.suck(clazz.openStream());
            className = name;
            bundleContainingOriginalClass = bundle;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public void setBytes(byte[] newBytes) {
            bytes = newBytes;
        }

        @Override
        public List<String> getDynamicImports() {
            return dynamicImports;
        }

        @Override
        public boolean isWeavingComplete() {
            return weavingComplete;
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public ProtectionDomain getProtectionDomain() {
            return null;
        }

        @Override
        public Class<?> getDefinedClass() {
            try {
                weavingComplete = true;
                return new MyWovenClassClassLoader(className, getBytes(), getClass().getClassLoader()).loadClass(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public BundleWiring getBundleWiring() {
            BundleWiring bw = EasyMock.createMock(BundleWiring.class);
            EasyMock.expect(bw.getBundle()).andReturn(bundleContainingOriginalClass);
            EasyMock.replay(bw);
            return bw;
        }
    }
    
    private static class MyWovenClassClassLoader extends ClassLoader {
        private final String className;
        private final byte [] bytes;
        
        public MyWovenClassClassLoader(String className, byte[] bytes, ClassLoader parent) {
            super(parent);
            
            this.className = className;
            this.bytes = bytes;            
        }
        
        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (name.equals(className)) {
                return defineClass(className, bytes, 0, bytes.length);
            } else {
                return super.loadClass(name, resolve);
            }
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return loadClass(name, false);
        }
    }    
}
