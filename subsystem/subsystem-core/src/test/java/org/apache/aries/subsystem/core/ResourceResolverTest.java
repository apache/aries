package org.apache.aries.subsystem.core;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.subsystem.core.internal.ResourceResolverImpl;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.obr.RepositoryAdmin;

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
    }
}
