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
package org.apache.aries.jmx.codec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osgi.jmx.framework.BundleStateMBean.ACTIVATION_POLICY_USED;
import static org.osgi.jmx.framework.BundleStateMBean.BUNDLES_TYPE;
import static org.osgi.jmx.framework.BundleStateMBean.BUNDLE_TYPE;
import static org.osgi.jmx.framework.BundleStateMBean.EXPORTED_PACKAGES;
import static org.osgi.jmx.framework.BundleStateMBean.FRAGMENT;
import static org.osgi.jmx.framework.BundleStateMBean.FRAGMENTS;
import static org.osgi.jmx.framework.BundleStateMBean.HEADERS;
import static org.osgi.jmx.framework.BundleStateMBean.HEADERS_TYPE;
import static org.osgi.jmx.framework.BundleStateMBean.HEADER_TYPE;
import static org.osgi.jmx.framework.BundleStateMBean.HOSTS;
import static org.osgi.jmx.framework.BundleStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.BundleStateMBean.IMPORTED_PACKAGES;
import static org.osgi.jmx.framework.BundleStateMBean.KEY;
import static org.osgi.jmx.framework.BundleStateMBean.LAST_MODIFIED;
import static org.osgi.jmx.framework.BundleStateMBean.LOCATION;
import static org.osgi.jmx.framework.BundleStateMBean.PERSISTENTLY_STARTED;
import static org.osgi.jmx.framework.BundleStateMBean.REGISTERED_SERVICES;
import static org.osgi.jmx.framework.BundleStateMBean.REMOVAL_PENDING;
import static org.osgi.jmx.framework.BundleStateMBean.REQUIRED;
import static org.osgi.jmx.framework.BundleStateMBean.REQUIRED_BUNDLES;
import static org.osgi.jmx.framework.BundleStateMBean.REQUIRING_BUNDLES;
import static org.osgi.jmx.framework.BundleStateMBean.SERVICES_IN_USE;
import static org.osgi.jmx.framework.BundleStateMBean.START_LEVEL;
import static org.osgi.jmx.framework.BundleStateMBean.STATE;
import static org.osgi.jmx.framework.BundleStateMBean.SYMBOLIC_NAME;
import static org.osgi.jmx.framework.BundleStateMBean.VALUE;
import static org.osgi.jmx.framework.BundleStateMBean.VERSION;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.aries.jmx.codec.BundleData.Header;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.service.startlevel.StartLevel;

/**
 *
 *
 * @version $Rev$ $Date$
 */
public class BundleDataTest {


    @Test
    public void testToCompositeData() throws Exception {

        Bundle bundle = mock(Bundle.class);
        BundleContext context = mock(BundleContext.class);
        PackageAdmin packageAdmin = mock(PackageAdmin.class);
        StartLevel startLevel = mock(StartLevel.class);

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

        when(bundle.getSymbolicName()).thenReturn("test");
        when(bundle.getVersion()).thenReturn(Version.emptyVersion);
        when(bundle.getBundleId()).thenReturn(new Long(1));
        when(bundle.getLastModified()).thenReturn(new Long(12345));
        when(bundle.getLocation()).thenReturn("location");

        //headers
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "test");
        headers.put(Constants.BUNDLE_VERSION, "0.0.0");
        when(bundle.getHeaders()).thenReturn(headers);

        //exported packages
        ExportedPackage exported = mock(ExportedPackage.class);
        when(exported.getName()).thenReturn("org.apache.aries.jmx");
        when(exported.getVersion()).thenReturn(new Version("1.0.0"));
        when(exported.getExportingBundle()).thenReturn(bundle);
        when(packageAdmin.getExportedPackages(bundle)).thenReturn(new ExportedPackage[] { exported });

        //imported packages
        ExportedPackage ep1 = mock(ExportedPackage.class);
        when(ep1.getImportingBundles()).thenReturn(new Bundle[] { bundle, b2, b3 });
        when(ep1.getName()).thenReturn("org.apache.aries.jmx.b1");
        when(ep1.getVersion()).thenReturn(Version.emptyVersion);
        when(ep1.getExportingBundle()).thenReturn(b1);
        ExportedPackage ep2 = mock(ExportedPackage.class);
        when(ep2.getImportingBundles()).thenReturn(new Bundle[] { bundle, b3 });
        when(ep2.getName()).thenReturn("org.apache.aries.jmx.b2");
        when(ep2.getVersion()).thenReturn(Version.parseVersion("2.0.1"));
        when(ep2.getExportingBundle()).thenReturn(b2);
        headers.put(Constants.DYNAMICIMPORT_PACKAGE, "*");

        when(packageAdmin.getExportedPackages(b1)).thenReturn(new ExportedPackage[] { ep1 });
        when(packageAdmin.getExportedPackages(b2)).thenReturn(new ExportedPackage[] { ep2 });
        when(packageAdmin.getExportedPackages(b3)).thenReturn(null);

        //required bundles
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
        when(packageAdmin.getRequiredBundles("b1")).thenReturn(new RequiredBundle[] { rb1 });
        when(packageAdmin.getRequiredBundles("b2")).thenReturn(new RequiredBundle[] { rb2 });
        when(packageAdmin.getRequiredBundles("b3")).thenReturn(new RequiredBundle[] { rb3 });

        //services in use
        ServiceReference s1 = mock(ServiceReference.class);
        when(s1.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(15));
        ServiceReference s2 = mock(ServiceReference.class);
        when(s2.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(16));
        ServiceReference s3 = mock(ServiceReference.class);
        when(s3.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(17));

        when(bundle.getServicesInUse()).thenReturn(new ServiceReference[] { s1, s2, s3 });

        BundleData b = new BundleData(context, bundle, packageAdmin, startLevel);
        CompositeData compositeData = b.toCompositeData();

        assertEquals("test", compositeData.get(SYMBOLIC_NAME));
        assertEquals("0.0.0", compositeData.get(VERSION));
        TabularData headerTable = (TabularData) compositeData.get(HEADERS);
        assertEquals(4, headerTable.values().size());
        CompositeData header = headerTable.get(new Object[]{Constants.BUNDLE_SYMBOLICNAME});
        assertNotNull(header);
        String value = (String) header.get(VALUE);
        assertEquals("test", value);
        String key = (String)header.get(KEY);
        assertEquals(Constants.BUNDLE_SYMBOLICNAME, key);


        TabularData bundleTable = new TabularDataSupport(BUNDLES_TYPE);
        bundleTable.put(b.toCompositeData());

        CompositeData bundleData = bundleTable.get(new Object[]{Long.valueOf(1)});
        assertNotNull(bundleData);
        String location = (String) bundleData.get(LOCATION);
        assertEquals("location", location);

        assertArrayEquals(new String[] { "org.apache.aries.jmx;1.0.0"} , (String[]) compositeData.get(EXPORTED_PACKAGES));
        assertArrayEquals(new String[] { "org.apache.aries.jmx.b1;0.0.0" , "org.apache.aries.jmx.b2;2.0.1"}, (String[]) compositeData.get(IMPORTED_PACKAGES));
        assertEquals(toSet(new long[] { 44, 55, 66 }), toSet((Long[]) compositeData.get(REQUIRED_BUNDLES)));
        assertArrayEquals(new Long[] { new Long(15), new Long(16), new Long(17) },(Long[]) compositeData.get(SERVICES_IN_USE));
        assertEquals("UNKNOWN", compositeData.get(STATE)); //default no return stub
        assertEquals(0,((Long[]) compositeData.get(HOSTS)).length);
        assertEquals(0, ((Long[]) compositeData.get(FRAGMENTS)).length);

    }


    @Test
    public void testFromCompositeData() throws Exception {

        Map<String, Object> items = new HashMap<String, Object>();
        items.put(EXPORTED_PACKAGES, new String[] { "org.apache.aries.jmx;1.0.0"});
        items.put(FRAGMENT, false);
        items.put(FRAGMENTS, new Long[0]);
        items.put(HOSTS, new Long[0]);
        items.put(IDENTIFIER, new Long(3));
        items.put(IMPORTED_PACKAGES, new String[] { "org.apache.aries.jmx.b1;0.0.0" , "org.apache.aries.jmx.b2;2.0.1"});
        items.put(LAST_MODIFIED, new Long(8797));
        items.put(LOCATION, "");
        items.put(ACTIVATION_POLICY_USED, true);
        items.put(PERSISTENTLY_STARTED, false);
        items.put(REGISTERED_SERVICES, new Long[0]);
        items.put(REMOVAL_PENDING, false);
        items.put(REQUIRED, true);
        items.put(REQUIRED_BUNDLES, new Long[] { new Long(44), new Long(66) });
        items.put(REQUIRING_BUNDLES, new Long[0]);
        items.put(SERVICES_IN_USE, new Long[] { new Long(15), new Long(16), new Long(17) });
        items.put(START_LEVEL, 1);
        items.put(STATE, "ACTIVE");
        items.put(SYMBOLIC_NAME, "test");
        items.put(VERSION, "0.0.0");
        TabularData headerTable = new TabularDataSupport(HEADERS_TYPE);
        headerTable.put(new Header("a", "a").toCompositeData());
        headerTable.put(new Header("b", "b").toCompositeData());
        items.put(HEADERS, headerTable);
        CompositeData compositeData = new CompositeDataSupport(BUNDLE_TYPE, items);

        BundleData b = BundleData.from(compositeData);

        assertEquals("test", b.getSymbolicName());
        assertEquals("0.0.0", b.getVersion());
        assertEquals(2, b.getHeaders().size());
        assertArrayEquals(new String[] { "org.apache.aries.jmx;1.0.0"} , b.getExportedPackages());
        assertArrayEquals(new String[] { "org.apache.aries.jmx.b1;0.0.0" , "org.apache.aries.jmx.b2;2.0.1"}, b.getImportedPackages());
        assertArrayEquals(new long[] { 44, 66 }, b.getRequiredBundles());
        assertArrayEquals(new long[] { 15, 16, 17 }, b.getServicesInUse());
        assertEquals("ACTIVE", b.getState()); //default no return stub
        assertEquals(0, b.getHosts().length);
        assertEquals(0, b.getFragments().length);
    }

    @Test
    public void testHeaderToCompositeData() throws Exception{

        Header h1 = new Header("a", "b");
        CompositeData compositeData = h1.toCompositeData();

        assertEquals("a", compositeData.get(KEY));
        assertEquals("b", compositeData.get(VALUE));

    }

    @Test
    public void testHeaderFromCompositeData() throws Exception {

        CompositeData compositeData = new CompositeDataSupport(HEADER_TYPE, new String[] { KEY, VALUE } , new String [] { "c", "d" });
        Header header = Header.from(compositeData);
        assertEquals("c", header.getKey());
        assertEquals("d", header.getValue());

    }

    private static Set<Long> toSet(long[] array) {
        Set<Long> set = new HashSet<Long>();
        for (long value : array) {
            set.add(value);
        }
        return set;
    }

    private static Set<Long> toSet(Long[] array) {
        Set<Long> set = new HashSet<Long>();
        for (Long value : array) {
            set.add(value);
        }
        return set;
    }
}
