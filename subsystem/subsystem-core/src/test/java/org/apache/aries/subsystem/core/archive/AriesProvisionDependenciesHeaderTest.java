package org.apache.aries.subsystem.core.archive;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AriesProvisionDependenciesHeaderTest {
    @Test
    public void testClauseGetValue() {
        assertTrue("resolve".equals(AriesProvisionDependenciesDirective.RESOLVE.getValue()));
        assertTrue("install".equals(AriesProvisionDependenciesDirective.INSTALL.getValue()));
    }

    @Test
    public void testClauseGetInstance() {
        assertTrue(AriesProvisionDependenciesDirective.getInstance("resolve")==
                AriesProvisionDependenciesDirective.RESOLVE);
        assertTrue(AriesProvisionDependenciesDirective.getInstance("install")==
                AriesProvisionDependenciesDirective.INSTALL);
    }
}
