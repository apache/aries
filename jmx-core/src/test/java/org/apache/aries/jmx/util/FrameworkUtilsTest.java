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
package org.apache.aries.jmx.util;

import static org.apache.aries.jmx.util.FrameworkUtils.getBundleDependencies;
import static org.apache.aries.jmx.util.FrameworkUtils.getBundleExportedPackages;
import static org.apache.aries.jmx.util.FrameworkUtils.getBundleIds;
import static org.apache.aries.jmx.util.FrameworkUtils.getBundleImportedPackages;
import static org.apache.aries.jmx.util.FrameworkUtils.getRegisteredServiceIds;
import static org.apache.aries.jmx.util.FrameworkUtils.getServiceIds;
import static org.apache.aries.jmx.util.FrameworkUtils.getServicesInUseByBundle;
import static org.apache.aries.jmx.util.FrameworkUtils.isBundlePendingRemoval;
import static org.apache.aries.jmx.util.FrameworkUtils.isBundleRequiredByOthers;
import static org.apache.aries.jmx.util.FrameworkUtils.resolveService;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

/**
 * 
 * 
 *
 * @version $Rev$ $Date$
 */
public class FrameworkUtilsTest {

   
    @Test
    public void testGetBundleIds() throws Exception {

        assertEquals(0, getBundleIds((Bundle[])null).length);
        assertEquals(0, getBundleIds(new Bundle[0]).length);
        
        Bundle b1 = mock(Bundle.class);
        when(b1.getBundleId()).thenReturn(new Long(47));
        Bundle b2 = mock(Bundle.class);
        when(b2.getBundleId()).thenReturn(new Long(23));
        
        assertArrayEquals(new long[] { 47 , 23 }, getBundleIds(new Bundle[] { b1, b2 }));
        
    }
    
    @Test
    public void testResolveService() throws Exception {
        
        BundleContext context = mock(BundleContext.class);
        ServiceReference reference = mock(ServiceReference.class);
        when(context.getAllServiceReferences(anyString(), anyString())).thenReturn(new ServiceReference[] { reference });        
        ServiceReference result = resolveService(context, 998);
        assertNotNull(result);
        
    }

    @Test
    public void testGetServiceIds() throws Exception {
        
        assertEquals(0, getServiceIds(null).length);
        assertEquals(0, getServiceIds(new ServiceReference[0]).length);
        
        ServiceReference s1 = mock(ServiceReference.class);
        when(s1.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(15));
        ServiceReference s2 = mock(ServiceReference.class);
        when(s2.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(5));
        ServiceReference s3 = mock(ServiceReference.class);
        when(s3.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(25));
        
        assertArrayEquals(new long[] { 15, 5, 25 }, 
                getServiceIds(new ServiceReference[] {s1, s2, s3} ) );
    }
    
    @Test
    public void testGetBundleExportedPackages() throws Exception {
        
        Bundle bundle = mock(Bundle.class);
        PackageAdmin admin = mock(PackageAdmin.class);
        
        assertEquals(0, getBundleExportedPackages(bundle, admin).length);
        
        ExportedPackage exported = mock(ExportedPackage.class);
        when(exported.getName()).thenReturn("org.apache.aries.jmx");
        when(exported.getVersion()).thenReturn(new Version("1.0.0"));
        when(admin.getExportedPackages(bundle)).thenReturn(new ExportedPackage[] { exported });
        
        assertArrayEquals(new String[] { "org.apache.aries.jmx;1.0.0"} , getBundleExportedPackages(bundle, admin));
        
    }
    
    
    @Test
    public void testGetBundleImportedPackages() throws Exception {
        
        Bundle bundle = mock(Bundle.class);
        BundleContext context = mock(BundleContext.class);
        
        Bundle b1 = mock(Bundle.class);
        Bundle b2 = mock(Bundle.class);
        Bundle b3 = mock(Bundle.class);
        when(context.getBundles()).thenReturn(new Bundle[] { bundle, b1, b2, b3 });
        
        ExportedPackage ep1 = mock(ExportedPackage.class);
        when(ep1.getImportingBundles()).thenReturn(new Bundle[] { bundle, b2, b3 });
        when(ep1.getName()).thenReturn("org.apache.aries.jmx.b1");
        when(ep1.getVersion()).thenReturn(Version.emptyVersion);
        ExportedPackage ep2 = mock(ExportedPackage.class);
        when(ep2.getImportingBundles()).thenReturn(new Bundle[] { bundle, b3 });
        when(ep2.getName()).thenReturn("org.apache.aries.jmx.b2");
        when(ep2.getVersion()).thenReturn(Version.parseVersion("2.0.1"));
        
        PackageAdmin admin = mock(PackageAdmin.class);
        when(admin.getExportedPackages(b1)).thenReturn(new ExportedPackage[] { ep1 });
        when(admin.getExportedPackages(b2)).thenReturn(new ExportedPackage[] { ep2 });
        when(admin.getExportedPackages(b3)).thenReturn(null);
        
        //check first with DynamicImport
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(Constants.DYNAMICIMPORT_PACKAGE, "*");
        when(bundle.getHeaders()).thenReturn(headers);
        assertArrayEquals(new String[] { "org.apache.aries.jmx.b1;0.0.0" , "org.apache.aries.jmx.b2;2.0.1"} 
                    , getBundleImportedPackages(context, bundle, admin));
        
        //check with ImportPackage statement
        headers.remove(Constants.DYNAMICIMPORT_PACKAGE);
        String importPackageStatement = "org.apache.aries.jmx.b1;version=0.0.0;resolution:=optional,org.apache.aries.jmx.b2;attribute:=value;version=\"[2.0, 3.0)\""; 
        headers.put(Constants.IMPORT_PACKAGE, importPackageStatement);
        when(admin.getExportedPackages("org.apache.aries.jmx.b1")).thenReturn(new ExportedPackage[] { ep1 });
        when(admin.getExportedPackages("org.apache.aries.jmx.b2")).thenReturn(new ExportedPackage[] { ep2 });
        
        assertArrayEquals(new String[] { "org.apache.aries.jmx.b1;0.0.0" , "org.apache.aries.jmx.b2;2.0.1"} 
                    , getBundleImportedPackages(context, bundle, admin));
        
        
    }
    
    @Test
    public void testGetRegisteredServiceIds() throws Exception {
        
        Bundle bundle = mock(Bundle.class);
        
        ServiceReference s1 = mock(ServiceReference.class);
        when(s1.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(56));
        ServiceReference s2 = mock(ServiceReference.class);
        when(s2.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(5));
        ServiceReference s3 = mock(ServiceReference.class);
        when(s3.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(34));
        
        when(bundle.getRegisteredServices()).thenReturn(new ServiceReference[] { s1, s2, s3 });
        
        assertArrayEquals(new long[] { 56, 5, 34}, getRegisteredServiceIds(bundle));
        
    }
    
    @Test
    public void testGetServicesInUseByBundle() throws Exception {
        
        Bundle bundle = mock(Bundle.class);
        
        ServiceReference s1 = mock(ServiceReference.class);
        when(s1.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(15));
        ServiceReference s2 = mock(ServiceReference.class);
        when(s2.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(16));
        ServiceReference s3 = mock(ServiceReference.class);
        when(s3.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(17));
        
        when(bundle.getServicesInUse()).thenReturn(new ServiceReference[] { s1, s2, s3 });
        
        assertArrayEquals(new long[] { 15, 16, 17 }, getServicesInUseByBundle(bundle));
        
    }
    
    @Test
    public void testIsBundlePendingRemoval() throws Exception {
        
        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn("org.apache.testb");
        
        RequiredBundle reqBundle = mock(RequiredBundle.class);
        when(reqBundle.getBundle()).thenReturn(bundle);
        when(reqBundle.isRemovalPending()).thenReturn(true);
        
        PackageAdmin admin = mock(PackageAdmin.class);
        when(admin.getRequiredBundles("org.apache.testb")).thenReturn(new RequiredBundle[] { reqBundle });
        
        assertTrue(isBundlePendingRemoval(bundle, admin));
        
    }
    
    @Test
    public void testIsBundleRequiredByOthers() throws Exception {
        
        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn("org.apache.testb");
        
        RequiredBundle reqBundle = mock(RequiredBundle.class);
        when(reqBundle.getBundle()).thenReturn(bundle);
        when(reqBundle.getRequiringBundles()).thenReturn(new Bundle[0]);
        
        PackageAdmin admin = mock(PackageAdmin.class);
        when(admin.getRequiredBundles("org.apache.testb")).thenReturn(new RequiredBundle[] { reqBundle });
        
        assertFalse(isBundleRequiredByOthers(bundle, admin));
        
        Bundle user = mock(Bundle.class);
        when(reqBundle.getRequiringBundles()).thenReturn(new Bundle[] { user });
        
        assertTrue(isBundleRequiredByOthers(bundle, admin));
    }
    
    
    @Test
    public void testGetBundleDependencies() throws Exception {
        
        Bundle bundle = mock(Bundle.class);
        BundleContext context = mock(BundleContext.class);
       
        Bundle b1 = mock(Bundle.class);
        when(b1.getSymbolicName()).thenReturn("b1");
        when(b1.getBundleId()).thenReturn(new Long(44));
        Bundle b2 = mock(Bundle.class);
        when(b2.getSymbolicName()).thenReturn("b2");
        when(b2.getBundleId()).thenReturn(new Long(55));
        Bundle b3 = mock(Bundle.class);
        when(b3.getSymbolicName()).thenReturn("b3");
        when(b3.getBundleId()).thenReturn(new Long(66));
        
        when(context.getBundles()).thenReturn(new Bundle[] { bundle, b1, b2, b3 });
        
        Dictionary<String, String> headers = new Hashtable<String, String>();
        when(bundle.getHeaders()).thenReturn(headers);
        
        PackageAdmin admin = mock(PackageAdmin.class);
        assertEquals(0, getBundleDependencies(context, bundle, admin).length);
        
        RequiredBundle rb1 = mock(RequiredBundle.class);
        when(rb1.getBundle()).thenReturn(b1);
        when(rb1.getRequiringBundles()).thenReturn(new Bundle[] { bundle, b2 });
        RequiredBundle rb2 = mock(RequiredBundle.class);
        when(rb2.getBundle()).thenReturn(b2);
        when(rb2.getRequiringBundles()).thenReturn(new Bundle[] { b1 });
        RequiredBundle rb3 = mock(RequiredBundle.class);
        when(rb3.getBundle()).thenReturn(b3);
        when(rb3.getRequiringBundles()).thenReturn(new Bundle[] { bundle, b1, b2 });
        
        headers.put(Constants.REQUIRE_BUNDLE, "b1;bundle-version=\"1.0.0\",b3;bundle-version=\"2.0.0\"");
        
        when(admin.getRequiredBundles("b1")).thenReturn(new RequiredBundle[] { rb1 });
        when(admin.getRequiredBundles("b2")).thenReturn(new RequiredBundle[] { rb2 });
        when(admin.getRequiredBundles("b3")).thenReturn(new RequiredBundle[] { rb3 });
        
        assertEquals(toSet(new long[] { 44, 66 }), toSet(getBundleDependencies(context, bundle, admin)));
    }
    
    private static Set<Long> toSet(long[] array) {
        Set<Long> set = new HashSet<Long>();
        for (long value : array) {
            set.add(value);
        }
        return set;
    }
    
}
