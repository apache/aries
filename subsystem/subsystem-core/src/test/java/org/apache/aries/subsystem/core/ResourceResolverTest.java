/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.subsystem.core.internal.ResourceResolverImpl;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.apache.felix.bundlerepository.RepositoryAdmin;

public class ResourceResolverTest {

    /** The bundle context for the test */
    private BundleContext ctx;
    
    @Before
    public void before() throws Exception {
        ctx = Skeleton.newMock(new BundleContextMock(), BundleContext.class);
        
        RepositoryAdmin ra = Skeleton.newMock(new MockRepositoryAdminImpl(),
                RepositoryAdmin.class);
        
        ctx.registerService(RepositoryAdmin.class.getCanonicalName(), ra, null);

        
        File file = new File(System.getProperty("user.home") + "/.m2/repository/repository.xml");
        if (file.exists()) {
            assertTrue(file.delete());
        }
    }
    @Test
    public void testGenerateRepo() throws Exception {
        ResourceResolverImpl rr = new ResourceResolverImpl(ctx);
        rr.generateOBR();
        File file = new File(System.getProperty("user.home") + "/.m2/repository/repository.xml");
        assertTrue("file " + file.toString() + " should exist after generate OBR", file.exists());
        file.delete();
    }
}
