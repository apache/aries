package org.apache.aries.spifly;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Assert;
import org.junit.Test;

public class UtilTest {
    @Test
    public void testSetRestoreTCCL() {
        ClassLoader cl = new URLClassLoader(new URL[] {});
        Thread.currentThread().setContextClassLoader(cl);
        Util.storeContextClassloader();
        
        Thread.currentThread().setContextClassLoader(null);
        
        Util.restoreContextClassloader();
        Assert.assertSame(cl, Thread.currentThread().getContextClassLoader());
    }
}
