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
package org.apache.aries.blueprint.itests.authz;

import static org.apache.aries.blueprint.itests.Helper.mvnBundle;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.security.PrivilegedAction;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;

import org.apache.aries.blueprint.itests.AbstractBlueprintIntegrationTest;
import org.apache.aries.blueprint.itests.Helper;
import org.apache.aries.blueprint.itests.authz.helper.JAASHelper;
import org.apache.aries.blueprint.itests.authz.testbundle.SecuredService;
import org.apache.aries.blueprint.itests.authz.testbundle.impl.SecuredServiceImpl;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Test calling a service that is secured using the blueprint-authz module.
 * 
 * Access is regulated using JEE security annotations
 * @see SecuredServiceImpl
 */
public class AuthorizationTest extends AbstractBlueprintIntegrationTest {
    @Inject
    SecuredService service;
    
    @Test
    public void testOnlyAdminOk() throws LoginException, BundleException {
        JAASHelper.doAs(new String[] {"admin"}, new CallOnlyAdmin());
    }
    
    @Test(expected = AccessControlException.class)
    public void testOnlyAdminDenied() throws LoginException, BundleException {
        JAASHelper.doAs(new String[] {"user"}, new CallOnlyAdmin());
    }
    
    @Test
    public void testUserAdndAdminOk() throws LoginException, BundleException {
        JAASHelper.doAs(new String[] {"admin"}, new CallUserAndAdmin());
        JAASHelper.doAs(new String[] {"user"}, new CallUserAndAdmin());
    }
    
    @Test(expected = AccessControlException.class)
    public void testUserAdndAdminDeniedForUnauthenticated() throws LoginException, BundleException {
        service.userAndAdmin("Hi");
    }
    
    @Test
    public void testAnyOneUnauthenticatedOk() throws LoginException, BundleException {
        service.anyOne("Hi");
    }
    
    @Test(expected = AccessControlException.class)
    public void testDenyAll() throws LoginException, BundleException {
        JAASHelper.doAs(new String[] {"admin"}, new CallNoOne());
    }
    
    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
            probe.setHeader(Constants.EXPORT_PACKAGE, SecuredService.class.getPackage().getName());
            probe.setHeader(Constants.IMPORT_PACKAGE, SecuredService.class.getPackage().getName());
            return probe;
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] configuration() throws IOException, LoginException, BundleException {
        InputStream testBundle = TinyBundles.bundle()
                .set(Constants.BUNDLE_SYMBOLICNAME, "authz")
                .add(SecuredServiceImpl.class)
                .add(SecuredService.class)
                .add("OSGI-INF/blueprint/authz.xml", this.getClass().getResourceAsStream("/authz.xml"))
                .set(Constants.EXPORT_PACKAGE, SecuredService.class.getPackage().getName())
                .set(Constants.IMPORT_PACKAGE, SecuredService.class.getPackage().getName())
                .build(TinyBundles.withBnd());

        return new Option[] {
            baseOptions(),
            CoreOptions.keepCaches(),
            Helper.blueprintBundles(),
            mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.authz"),
            streamBundle(testBundle),
        };
    }
    
    private final class CallUserAndAdmin implements PrivilegedAction<Void> {
        @Override
        public Void run() {
            service.userAndAdmin("Hi");
            return null;
        }
    }

    private final class CallOnlyAdmin implements PrivilegedAction<Void> {
        @Override
        public Void run() {
            service.onlyAdmin("Hi");
            return null;
        }
    }
    
    private final class CallNoOne implements PrivilegedAction<Void> {
        @Override
        public Void run() {
            service.noOne("Hi");
            return null;
        }
    }

}