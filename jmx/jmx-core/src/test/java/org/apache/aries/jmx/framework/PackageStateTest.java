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
import java.util.Collection;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.jmx.framework.PackageStateMBean;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * {@link PackageStateMBean} test case.
 * 
 *
 * @version $Rev$ $Date$
 */
public class PackageStateTest {
    
    @Mock
    private BundleContext context;
    @Mock
    private PackageAdmin admin;
    private PackageState mbean;
    

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mbean = new PackageState(context, admin);
    }

    @Test
    public void testGetExportingBundles() throws IOException {
        ExportedPackage exported = Mockito.mock(ExportedPackage.class);
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(exported.getVersion()).thenReturn(Version.parseVersion("1.0.0"));
        Mockito.when(exported.getExportingBundle()).thenReturn(bundle);
        Mockito.when(bundle.getBundleId()).thenReturn(Long.valueOf(5));
        ExportedPackage exported2 = Mockito.mock(ExportedPackage.class);
        Bundle bundle2 = Mockito.mock(Bundle.class);
        Mockito.when(exported2.getVersion()).thenReturn(Version.parseVersion("1.0.0"));
        Mockito.when(exported2.getExportingBundle()).thenReturn(bundle2);
        Mockito.when(bundle2.getBundleId()).thenReturn(Long.valueOf(6));
        Mockito.when(admin.getExportedPackages(Mockito.anyString())).thenReturn(new ExportedPackage[]{exported, exported2});
        long[] ids = mbean.getExportingBundles("test", "1.0.0");
        Assert.assertNotNull(ids);
        Assert.assertArrayEquals(new long[]{5,6}, ids);
    }

    @Test
    public void testGetImportingBundles() throws IOException {
        ExportedPackage exported = Mockito.mock(ExportedPackage.class);
        Bundle bundle = Mockito.mock(Bundle.class);
        Bundle exportingBundle = Mockito.mock(Bundle.class);
        Mockito.when(exported.getVersion()).thenReturn(Version.parseVersion("1.0.0"));
        Mockito.when(exported.getExportingBundle()).thenReturn(exportingBundle);
        Mockito.when(exportingBundle.getBundleId()).thenReturn(Long.valueOf(2));
        Mockito.when(exported.getImportingBundles()).thenReturn(new Bundle[]{bundle});
        Mockito.when(bundle.getBundleId()).thenReturn(Long.valueOf(4));
        Mockito.when(admin.getExportedPackages(Mockito.anyString())).thenReturn(new ExportedPackage[]{exported});
        long[] ids = mbean.getImportingBundles("test", "1.0.0", 2);
        Assert.assertArrayEquals(new long[]{4}, ids);
    }

    @Test
    public void testIsRemovalPending() throws IOException {
        ExportedPackage exported = Mockito.mock(ExportedPackage.class);
        Bundle expBundle = Mockito.mock(Bundle.class);
        Mockito.when(exported.getVersion()).thenReturn(Version.parseVersion("1.0.0"));
        Mockito.when(exported.isRemovalPending()).thenReturn(true);
        Mockito.when(exported.getExportingBundle()).thenReturn(expBundle);
        Mockito.when(expBundle.getBundleId()).thenReturn(Long.valueOf(2));
        Mockito.when(admin.getExportedPackages(Mockito.anyString())).thenReturn(new ExportedPackage[]{exported});
        boolean isRemoval = mbean.isRemovalPending("test", "1.0.0", Long.valueOf(2));
        Assert.assertTrue(isRemoval);
    }

    @Test
    public void testListPackages() throws IOException {
        Bundle bundle = Mockito.mock(Bundle.class);
        Bundle impBundle = Mockito.mock(Bundle.class);
        Mockito.when(context.getBundles()).thenReturn(new Bundle[]{bundle});
        ExportedPackage exported = Mockito.mock(ExportedPackage.class);
        Mockito.when(exported.getVersion()).thenReturn(Version.parseVersion("1.0.0"));
        Mockito.when(exported.getImportingBundles()).thenReturn(new Bundle[]{impBundle});
        Mockito.when(exported.getName()).thenReturn("test");
        Mockito.when(exported.getExportingBundle()).thenReturn(bundle);
        Mockito.when(bundle.getBundleId()).thenReturn(Long.valueOf(4));
        Mockito.when(impBundle.getBundleId()).thenReturn(Long.valueOf(5));
        Mockito.when(admin.getExportedPackages(bundle)).thenReturn(new ExportedPackage[]{exported});
        TabularData table = mbean.listPackages();
        Assert.assertEquals(PackageStateMBean.PACKAGES_TYPE,table.getTabularType());
        Collection values = table.values();
        Assert.assertEquals(1, values.size());
        CompositeData data = (CompositeData) values.iterator().next();
        Long[] exportingBundles = (Long[])data.get(PackageStateMBean.EXPORTING_BUNDLES);
        Assert.assertArrayEquals(new Long[]{Long.valueOf(4)}, exportingBundles);
        String name = (String) data.get(PackageStateMBean.NAME);
        Assert.assertEquals("test", name);
        String version = (String) data.get(PackageStateMBean.VERSION);
        Assert.assertEquals("1.0.0", version);
    }

}
