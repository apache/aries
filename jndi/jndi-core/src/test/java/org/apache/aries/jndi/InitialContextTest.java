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
package org.apache.aries.jndi;

import junit.framework.Assert;
import org.apache.aries.jndi.startup.Activator;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.jndi.JNDIConstants;

import javax.naming.*;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.ObjectFactory;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class InitialContextTest {
    private Activator activator;
    private BundleContext bc;
    private InitialContext ic;

    /**
     * This method does the setup .
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Before
    public void setup() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        BundleContextMock mock = new BundleContextMock();
        mock.addBundle(mock.getBundle());
        bc = Skeleton.newMock(mock, BundleContext.class);
        activator = new Activator();
        activator.start(bc);
    }

    /**
     * Make sure we clear the caches out before the next test.
     */
    @After
    public void teardown() {
        activator.stop(bc);
        BundleContextMock.clear();
    }

    @Test
    public void testLookupWithICF() throws NamingException {
        InitialContextFactory icf = Skeleton.newMock(InitialContextFactory.class);
        bc.registerService(new String[]{InitialContextFactory.class.getName(), icf.getClass().getName()}, icf, (Dictionary) new Properties());
        Skeleton.getSkeleton(icf).setReturnValue(new MethodCall(Context.class, "lookup", "/"), Skeleton.newMock(Context.class));

        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, icf.getClass().getName());
        props.put(JNDIConstants.BUNDLE_CONTEXT, bc);
        InitialContext ctx = new InitialContext(props);

        Context namingCtx = (Context) ctx.lookup("/");
        assertTrue("Context returned isn't the raw naming context: " + namingCtx, Skeleton.isSkeleton(namingCtx));
    }

    @Test(expected = NoInitialContextException.class)
    public void testLookupWithoutICF() throws NamingException {
        Properties props = new Properties();
        props.put(JNDIConstants.BUNDLE_CONTEXT, bc);
        InitialContext ctx = new InitialContext(props);

        ctx.lookup("/");
    }

    @Test
    public void testLookupWithoutICFButWithURLLookup() throws NamingException {
        ObjectFactory factory = Skeleton.newMock(ObjectFactory.class);
        Context ctx = Skeleton.newMock(Context.class);
        Skeleton.getSkeleton(factory).setReturnValue(new MethodCall(ObjectFactory.class, "getObjectInstance", Object.class, Name.class, Context.class, Hashtable.class),
                ctx);
        Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(Context.class, "lookup", String.class), "someText");

        Properties props = new Properties();
        props.put(JNDIConstants.JNDI_URLSCHEME, "testURL");
        bc.registerService(ObjectFactory.class.getName(), factory, (Dictionary) props);


        props = new Properties();
        props.put(JNDIConstants.BUNDLE_CONTEXT, bc);
        InitialContext initialCtx = new InitialContext(props);

        Object someObject = initialCtx.lookup("testURL:somedata");
        assertEquals("Expected to be given a string, but got something else.", "someText", someObject);
    }

    @Test
    public void testLookFromLdapICF() throws Exception {
        InitialContextFactoryBuilder icf = Skeleton.newMock(InitialContextFactoryBuilder.class);
        bc.registerService(new String[]{InitialContextFactoryBuilder.class.getName(), icf.getClass().getName()}, icf, (Dictionary) new Properties());

        LdapContext backCtx = Skeleton.newMock(LdapContext.class);
        InitialContextFactory fac = Skeleton.newMock(InitialContextFactory.class);
        Skeleton.getSkeleton(fac).setReturnValue(
                new MethodCall(InitialContextFactory.class, "getInitialContext", Hashtable.class),
                backCtx);
        Skeleton.getSkeleton(icf).setReturnValue(
                new MethodCall(InitialContextFactoryBuilder.class, "createInitialContextFactory", Hashtable.class),
                fac);

        Properties props = new Properties();
        props.put(JNDIConstants.BUNDLE_CONTEXT, bc);
        props.put(Context.INITIAL_CONTEXT_FACTORY, "dummy.factory");
        InitialLdapContext ilc = new InitialLdapContext(props, new Control[0]);

        ExtendedRequest req = Skeleton.newMock(ExtendedRequest.class);
        ilc.extendedOperation(req);
        Skeleton.getSkeleton(backCtx).assertCalled(new MethodCall(LdapContext.class, "extendedOperation", req));
    }

    @Test
    public void testURLLookup() throws Exception {
        ObjectFactory of = new ObjectFactory() {
            public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
                return dummyContext("result");
            }
        };

        registerURLObjectFactory(of, "test");
        ic = initialContext();

        assertEquals("result", ic.lookup("test:something"));
    }

    @Test
    public void testNoURLContextCaching() throws Exception {
        final AtomicBoolean second = new AtomicBoolean(false);
        final Context ctx = dummyContext("one");
        final Context ctx2 = dummyContext("two");

        ObjectFactory of = new ObjectFactory() {
            public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
                if (second.get()) return ctx2;
                else {
                    second.set(true);
                    return ctx;
                }
            }
        };

        registerURLObjectFactory(of, "test");
        ic = initialContext();

        assertEquals("one", ic.lookup("test:something"));
        assertEquals("two", ic.lookup("test:something"));
    }

    @Test
    public void testURLContextErrorPropagation() throws Exception {
        ObjectFactory of = new ObjectFactory() {
            public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                            Hashtable<?, ?> environment) throws Exception {
                throw new Exception("doh");
            }
        };

        registerURLObjectFactory(of, "test");
        ic = initialContext();

        try {
            ic.lookup("test:something");
            Assert.fail("Expected NamingException");
        } catch (NamingException ne) {
            assertNotNull(ne.getCause());
            assertEquals("doh", ne.getCause().getMessage());
        }
    }

    /**
     * Create a minimal initial context with just the bundle context in the environment
     * @return
     * @throws Exception
     */
    private InitialContext initialContext() throws Exception {
        Properties props = new Properties();
        props.put(JNDIConstants.BUNDLE_CONTEXT, bc);
        InitialContext ic = new InitialContext(props);
        return ic;
    }

    /**
     * Registers an ObjectFactory to be used for creating URLContexts for the given scheme
     * @param of
     * @param scheme
     */
    private void registerURLObjectFactory(ObjectFactory of, String scheme) {
        Properties props = new Properties();
        props.setProperty(JNDIConstants.JNDI_URLSCHEME, "test");
        bc.registerService(ObjectFactory.class.getName(), of, (Dictionary) props);
    }

    /**
     * Creates a context that always returns the given object
     * @param toReturn
     * @return
     */
    private Context dummyContext(Object toReturn) {
        Context ctx = Skeleton.newMock(Context.class);
        Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(Context.class, "lookup", String.class), toReturn);
        Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(Context.class, "lookup", Name.class), toReturn);
        return ctx;
    }
}
