/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jmx.framework;

import java.io.IOException;
import java.io.InputStream;

import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.codec.BatchActionResult;
import org.apache.aries.jmx.codec.BatchInstallResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.jmx.framework.FrameworkMBean;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link FrameworkMBean} test case.
 *
 *
 * @version $Rev$ $Date$
 */
public class FrameworkTest {

    @Mock
    private StartLevel startLevel;
    @Mock
    private PackageAdmin admin;
    @Mock
    private BundleContext context;
    private Framework mbean;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mbean = new Framework(context, startLevel, admin);
    }

    @Test
    public void testGetFrameworkStartLevel() throws IOException {
        Mockito.when(startLevel.getStartLevel()).thenReturn(1);
        int level = mbean.getFrameworkStartLevel();
        assertEquals(1, level);
    }

    @Test
    public void testGetInitialBundleStartLevel() throws IOException {
        Mockito.when(startLevel.getInitialBundleStartLevel()).thenReturn(2);
        int level = mbean.getInitialBundleStartLevel();
        Mockito.verify(startLevel).getInitialBundleStartLevel();
        assertEquals(2, level);
    }

    @Test
    public void testInstallBundle() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.installBundle("file:test.jar")).thenReturn(bundle);
        Mockito.when(bundle.getBundleId()).thenReturn(Long.valueOf(2));
        long bundleId = mbean.installBundle("file:test.jar");
        assertEquals(2, bundleId);
        Mockito.reset(context);
        Mockito.when(context.installBundle("file:test2.jar")).thenThrow(new BundleException("location doesn't exist"));

        try {
            mbean.installBundle("file:test2.jar");
            fail("Shouldn't go to this stage, location doesn't exist");
        } catch (IOException e) {
            // ok
        }

    }

    @Test
    public void testInstallBundleFromURL() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.installBundle(Mockito.anyString(), Mockito.any(InputStream.class))).thenReturn(bundle);
        Mockito.when(bundle.getBundleId()).thenReturn(Long.valueOf(2));
        Framework spiedMBean = Mockito.spy(mbean);
        InputStream stream = Mockito.mock(InputStream.class);
        Mockito.doReturn(stream).when(spiedMBean).createStream("test.jar");
        long bundleId = spiedMBean.installBundleFromURL("file:test.jar", "test.jar");
        assertEquals(2, bundleId);
        Mockito.reset(context);
        Mockito.doReturn(stream).when(spiedMBean).createStream(Mockito.anyString());
        Mockito.when(context.installBundle(Mockito.anyString(), Mockito.any(InputStream.class))).thenThrow(
                new BundleException("location doesn't exist"));

        try {
            spiedMBean.installBundleFromURL("file:test2.jar", "test.jar");
            fail("Shouldn't go to this stage, location doesn't exist");
        } catch (IOException e) {
            // ok
        }
    }

    @Test
    public void testInstallBundles() throws Exception {
        String[] locations = new String[] { "file:test.jar" };
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.installBundle("file:test.jar")).thenReturn(bundle);
        Mockito.when(bundle.getBundleId()).thenReturn(Long.valueOf(2));
        CompositeData data = mbean.installBundles(locations);
        BatchInstallResult batch = BatchInstallResult.from(data);
        assertNotNull(batch);
        assertEquals(2, batch.getCompleted()[0]);
        assertTrue(batch.isSuccess());
        assertNull(batch.getError());
        assertNull(batch.getRemainingLocationItems());
        Mockito.reset(context);
        Mockito.when(context.installBundle("file:test.jar")).thenThrow(new BundleException("location doesn't exist"));
        CompositeData data2 = mbean.installBundles(locations);
        BatchInstallResult batch2 = BatchInstallResult.from(data2);
        assertNotNull(batch2);
        assertNotNull(batch2.getCompleted());
        assertEquals(0, batch2.getCompleted().length);
        assertFalse(batch2.isSuccess());
        assertNotNull(batch2.getError());
        assertEquals("file:test.jar", batch2.getBundleInError());
        assertNotNull(batch2.getRemainingLocationItems());
        assertEquals(0, batch2.getRemainingLocationItems().length);
    }

    @Test
    public void testInstallBundlesFromURL() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.installBundle(Mockito.anyString(), Mockito.any(InputStream.class))).thenReturn(bundle);
        Mockito.when(bundle.getBundleId()).thenReturn(Long.valueOf(2));
        Framework spiedMBean = Mockito.spy(mbean);
        InputStream stream = Mockito.mock(InputStream.class);
        Mockito.doReturn(stream).when(spiedMBean).createStream(Mockito.anyString());
        CompositeData data = spiedMBean.installBundlesFromURL(new String[] { "file:test.jar" }, new String[] { "test.jar" });
        assertNotNull(data);
        BatchInstallResult batch = BatchInstallResult.from(data);
        assertEquals(2, batch.getCompleted()[0]);
        assertTrue(batch.isSuccess());
        assertNull(batch.getError());
        assertNull(batch.getRemainingLocationItems());
        Mockito.reset(context);
        Mockito.when(spiedMBean.createStream(Mockito.anyString())).thenReturn(stream);
        Mockito.when(context.installBundle(Mockito.anyString(), Mockito.any(InputStream.class))).thenThrow(
                new BundleException("location doesn't exist"));
        CompositeData data2 = spiedMBean.installBundlesFromURL(new String[] { "file:test.jar" }, new String[] { "test.jar" });
        BatchInstallResult batch2 = BatchInstallResult.from(data2);
        assertNotNull(batch2);
        assertNotNull(batch2.getCompleted());
        assertEquals(0, batch2.getCompleted().length);
        assertFalse(batch2.isSuccess());
        assertNotNull(batch2.getError());
        assertEquals("file:test.jar", batch2.getBundleInError());
        assertNotNull(batch2.getRemainingLocationItems());
        assertEquals(0, batch2.getRemainingLocationItems().length);
    }

    @Test
    public void testRefreshBundle() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(1)).thenReturn(bundle);

        mbean.refreshBundle(1);
        Mockito.verify(admin).refreshPackages((Bundle[]) Mockito.any());

        try {
            mbean.refreshBundle(2);
            fail("IOException should be thrown");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testRefreshBundles() throws IOException {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(1)).thenReturn(bundle);

        mbean.refreshBundles(new long[] { 1 });
        Mockito.verify(admin).refreshPackages((Bundle[]) Mockito.any());

        mbean.refreshBundles(null);
        Mockito.verify(admin).refreshPackages(null);
    }

    @Test
    public void testResolveBundle() throws IOException {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(1)).thenReturn(bundle);

        mbean.resolveBundle(1);
        Mockito.verify(admin).resolveBundles(new Bundle[] { bundle });
    }

    @Test
    public void testResolveBundles() throws IOException {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(1)).thenReturn(bundle);
//        Mockito.when(context.getBundles()).thenReturn(new Bundle [] { bundle });

        mbean.resolveBundles(new long[] { 1 });
        Mockito.verify(admin).resolveBundles(new Bundle[] { bundle });

        mbean.resolveBundles(null);
        Mockito.verify(admin).resolveBundles(null);
    }

    @Test
    public void testRestartFramework() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(0)).thenReturn(bundle);
        mbean.restartFramework();
        Mockito.verify(bundle).update();
    }

    @Test
    public void testSetBundleStartLevel() throws IOException {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(2)).thenReturn(bundle);
        mbean.setBundleStartLevel(2, 1);
        Mockito.verify(startLevel).setBundleStartLevel(bundle, 1);
    }

    @Test
    public void testSetBundleStartLevels() throws IOException {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(2)).thenReturn(bundle);
        CompositeData data = mbean.setBundleStartLevels(new long[] { 2 }, new int[] { 2 });
        Mockito.verify(startLevel).setBundleStartLevel(bundle, 2);
        BatchActionResult batch = BatchActionResult.from(data);
        assertEquals(2, batch.getCompleted()[0]);
        assertTrue(batch.isSuccess());
        assertNull(batch.getError());
        assertNull(batch.getRemainingItems());

        CompositeData data2 = mbean.setBundleStartLevels(new long[] { 2 }, new int[] { 2, 4 });
        BatchActionResult batch2 = BatchActionResult.from(data2);
        assertNull(batch2.getCompleted());
        assertFalse(batch2.isSuccess());
        assertNotNull(batch2.getError());
        assertNull(batch2.getRemainingItems());

    }

    @Test
    public void testSetFrameworkStartLevel() throws IOException {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(0)).thenReturn(bundle);
        mbean.setFrameworkStartLevel(1);
        Mockito.verify(startLevel).setStartLevel(1);

    }

    @Test
    public void testSetInitialBundleStartLevel() throws IOException {
        mbean.setInitialBundleStartLevel(5);
        Mockito.verify(startLevel).setInitialBundleStartLevel(5);
    }

    @Test
    public void testShutdownFramework() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(0)).thenReturn(bundle);
        mbean.shutdownFramework();
        Mockito.verify(bundle).stop();
    }

    @Test
    public void testStartBundle() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(5)).thenReturn(bundle);
        mbean.startBundle(5);
        Mockito.verify(bundle).start();

        Mockito.reset(context);
        Mockito.when(context.getBundle(6)).thenReturn(bundle);
        Mockito.doThrow(new BundleException("")).when(bundle).start();

        try {
            mbean.startBundle(6);
            fail("Shouldn't go to this stage, BundleException was thrown");
        } catch (IOException ioe) {
            // expected
        }

        Mockito.when(context.getBundle(6)).thenReturn(null);
        try {
            mbean.startBundle(6);
            fail("IOException should be thrown");
        } catch (IOException e) {
            //expected
        }
    }

    @Test
    public void testStartBundles() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(5)).thenReturn(bundle);
        CompositeData data = mbean.startBundles(new long[] { 5 });
        Mockito.verify(bundle).start();

        BatchActionResult batch = BatchActionResult.from(data);
        assertEquals(5, batch.getCompleted()[0]);
        assertTrue(batch.isSuccess());
        assertNull(batch.getError());
        assertNull(batch.getRemainingItems());

        CompositeData data2 = mbean.startBundles(null);

        BatchActionResult batch2 = BatchActionResult.from(data2);
        assertNull(batch2.getCompleted());
        assertFalse(batch2.isSuccess());
        assertNotNull(batch2.getError());
        assertNull(batch2.getRemainingItems());
    }

    @Test
    public void testStopBundle() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(5)).thenReturn(bundle);
        mbean.stopBundle(5);
        Mockito.verify(bundle).stop();

        Mockito.when(context.getBundle(5)).thenReturn(null);
        try {
            mbean.stopBundle(5);
            fail("IOException should be thrown");
        } catch (IOException e) {
            //expected
        }

    }

    @Test
    public void testStopBundles() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(5)).thenReturn(bundle);
        CompositeData data = mbean.stopBundles(new long[] { 5 });
        Mockito.verify(bundle).stop();

        BatchActionResult batch = BatchActionResult.from(data);
        assertEquals(5, batch.getCompleted()[0]);
        assertTrue(batch.isSuccess());
        assertNull(batch.getError());
        assertNull(batch.getRemainingItems());

        CompositeData data2 = mbean.stopBundles(null);

        BatchActionResult batch2 = BatchActionResult.from(data2);
        assertNull(batch2.getCompleted());
        assertFalse(batch2.isSuccess());
        assertNotNull(batch2.getError());
        assertNull(batch2.getRemainingItems());
    }

    @Test
    public void testUninstallBundle() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(5)).thenReturn(bundle);
        mbean.uninstallBundle(5);
        Mockito.verify(bundle).uninstall();
    }

    @Test
    public void testUninstallBundles() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(5)).thenReturn(bundle);
        CompositeData data = mbean.uninstallBundles(new long[] { 5 });
        Mockito.verify(bundle).uninstall();
        BatchActionResult batch = BatchActionResult.from(data);
        assertEquals(5, batch.getCompleted()[0]);
        assertTrue(batch.isSuccess());
        assertNull(batch.getError());
        assertNull(batch.getRemainingItems());

        CompositeData data2 = mbean.uninstallBundles(null);

        BatchActionResult batch2 = BatchActionResult.from(data2);
        assertNull(batch2.getCompleted());
        assertFalse(batch2.isSuccess());
        assertNotNull(batch2.getError());
        assertNull(batch2.getRemainingItems());
    }

    @Test
    public void testUpdateBundle() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(5)).thenReturn(bundle);
        mbean.updateBundle(5);
        Mockito.verify(bundle).update();
    }

    @Test
    public void testUpdateBundleFromUrl() throws Exception {
        Framework spiedMBean = Mockito.spy(mbean);
        InputStream stream = Mockito.mock(InputStream.class);
        Mockito.doReturn(stream).when(spiedMBean).createStream(Mockito.anyString());
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(5)).thenReturn(bundle);
        spiedMBean.updateBundleFromURL(5, "file:test.jar");
        Mockito.verify(bundle).update(stream);
    }

    @Test
    public void testUpdateBundles() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(5)).thenReturn(bundle);
        CompositeData data = mbean.updateBundles(new long[] { 5 });
        Mockito.verify(bundle).update();
        BatchActionResult batch = BatchActionResult.from(data);
        assertEquals(5, batch.getCompleted()[0]);
        assertTrue(batch.isSuccess());
        assertNull(batch.getError());
        assertNull(batch.getRemainingItems());

        CompositeData data2 = mbean.updateBundles(null);

        BatchActionResult batch2 = BatchActionResult.from(data2);
        assertNull(batch2.getCompleted());
        assertFalse(batch2.isSuccess());
        assertNotNull(batch2.getError());
        assertNull(batch2.getRemainingItems());

        Mockito.reset(bundle);
        CompositeData data3 = mbean.updateBundles(new long[] { 6 });
        Mockito.when(context.getBundle(6)).thenReturn(bundle);
        Mockito.doThrow(new BundleException("")).when(bundle).update();
        BatchActionResult batch3 = BatchActionResult.from(data3);
        assertEquals(0, batch3.getCompleted().length);
        assertFalse(batch3.isSuccess());
        assertNotNull(batch3.getError());
        assertEquals(6, batch3.getBundleInError());

        Bundle bundle6 = Mockito.mock(Bundle.class);
        Bundle bundle8 = Mockito.mock(Bundle.class);
        Bundle bundle7 = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(6)).thenReturn(bundle6);
        Mockito.when(context.getBundle(8)).thenReturn(bundle8);
        Mockito.when(context.getBundle(7)).thenReturn(bundle7);
        Mockito.doThrow(new BundleException("")).when(bundle8).update();
        CompositeData data4 = mbean.updateBundles(new long[] { 6, 8, 7 });
        BatchActionResult batch4 = BatchActionResult.from(data4);
        Mockito.verify(bundle6).update();
        assertEquals(1, batch4.getCompleted().length);
        // should contain only bundleid 6
        assertEquals(6, batch4.getCompleted()[0]);
        assertFalse(batch4.isSuccess());
        assertNotNull(batch4.getError());
        assertEquals(8, batch4.getBundleInError());
        assertEquals(1, batch4.getRemainingItems().length);
        // should contain only bundleid 7
        assertEquals(7, batch4.getRemainingItems()[0]);
    }

    @Test
    public void testUpdateBundlesFromURL() throws Exception {
        Framework spiedMBean = Mockito.spy(mbean);
        InputStream stream = Mockito.mock(InputStream.class);
        Mockito.doReturn(stream).when(spiedMBean).createStream(Mockito.anyString());
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(5)).thenReturn(bundle);
        CompositeData data = spiedMBean.updateBundlesFromURL(new long[] { 5 }, new String[] { "file:test.jar" });
        Mockito.verify(bundle).update(stream);
        BatchActionResult batch = BatchActionResult.from(data);
        assertEquals(5, batch.getCompleted()[0]);
        assertTrue(batch.isSuccess());
        assertNull(batch.getError());
        assertNull(batch.getRemainingItems());

        CompositeData data2 = spiedMBean.updateBundlesFromURL(new long[] { 2, 4 }, new String[] { "file:test.jar" });
        BatchActionResult batch2 = BatchActionResult.from(data2);
        assertFalse(batch2.isSuccess());
        assertNotNull(batch2.getError());
        assertNotNull(batch2.getError());
        assertNull(batch2.getRemainingItems());
    }

    @Test
    public void testUpdateFramework() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundle(0)).thenReturn(bundle);
        mbean.restartFramework();
        Mockito.verify(bundle).update();
    }

}
