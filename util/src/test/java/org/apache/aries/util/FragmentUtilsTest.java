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
package org.apache.aries.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.mocks.BundleMock;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

@SuppressWarnings("rawtypes")
public class FragmentUtilsTest {

    private Bundle hostBundle;

    @Before
    public void setUp() throws Exception {
        hostBundle = Skeleton.newMock(new BundleMock("scooby.doo",
                new Hashtable<String, Object>()), Bundle.class);
    }

    @Test
    public void testFragmentCreation() throws Exception {
        Bundle exportBundle = makeBundleWithExports("export.bundle", "1.2.3",
                "export.package;version=\"1.0.0\";uses:=\"foo.jar,bar.jar\";singleton:=true");

        Dictionary fragmentHeaders = makeFragmentFromExportBundle(exportBundle)
                .getHeaders();

        assertNotNull("No headers in the fragment", fragmentHeaders);

        assertEquals("Wrong symbolicName", "scooby.doo.test.fragment",
                    fragmentHeaders.get(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("Wrong version", "0.0.0", 
                    fragmentHeaders.get(Constants.BUNDLE_VERSION));
        assertEquals("Wrong Bundle manifest version", "2", 
                    fragmentHeaders.get(Constants.BUNDLE_MANIFESTVERSION));
        assertEquals("Wrong Fragment host", "scooby.doo;bundle-version=\"0.0.0\"", 
                    fragmentHeaders.get(Constants.FRAGMENT_HOST));
        assertEquals("Wrong Bundle Name", "Test Fragment bundle",
                    fragmentHeaders.get(Constants.BUNDLE_NAME));
        assertEquals(
                "Wrong Imports",
                "export.package;version=\"1.0.0\";bundle-symbolic-name=\"export.bundle\";bundle-version=\"[1.2.3,1.2.3]\"",
                fragmentHeaders.get(Constants.IMPORT_PACKAGE));
    }

    private Bundle makeBundleWithExports(String symbolicName, String version,
            String exports) {
        Hashtable<String, Object> headers = new Hashtable<String, Object>();
        headers.put(Constants.BUNDLE_VERSION, version);
        headers.put(Constants.EXPORT_PACKAGE, exports);
        Bundle exportBundle = Skeleton.newMock(new BundleMock(symbolicName,
                headers), Bundle.class);
        return exportBundle;
    }

    private Bundle makeFragmentFromExportBundle(Bundle exportBundle)
            throws Exception {
        FragmentBuilder builder = new FragmentBuilder(hostBundle, "test.fragment", "fragment");
        builder.setName("Test Fragment bundle");
        builder.addImportsFromExports(exportBundle);

        return builder.install(hostBundle.getBundleContext());
    }

    @Test
    public void testManifestAttributes() throws Exception {
        String fakeExportsListNoExtras = "no.such.export,no.such.export2";
        String fakeExportsListAttrOnly = "no.such.export;version=\"1.1.1\",no.such.export2;version=\"2.2.2\"";
        String fakeExportsListDirOnly = "no.such.export;uses:=\"some.other.thing\",no.such.export2;include:=\"some.thing\"";
        String fakeExportsListMixed = "no.such.export;version=\"1.1.1\";uses:=\"some.other.thing\",no.such.export2;include:=\"some.thing\"";
        String fakeExportsListFunkyAttr = "no.such.export;attribute=\"a:=\",no.such.export2;attributeTwo=\"b:=\";include:=\"some.thing\"";

        String expectedImportsListNoExtras = "no.such.export;bundle-symbolic-name=\"no.such.provider\";bundle-version=\"[1.1.1,1.1.1]\",no.such.export2;bundle-symbolic-name=\"no.such.provider\";bundle-version=\"[1.1.1,1.1.1]\"";
        String expectedImportsListAttrOnly = "no.such.export;version=\"1.1.1\";bundle-symbolic-name=\"no.such.provider\";bundle-version=\"[1.1.1,1.1.1]\",no.such.export2;version=\"2.2.2\";bundle-symbolic-name=\"no.such.provider\";bundle-version=\"[1.1.1,1.1.1]\"";
        String expectedImportsListDirOnly = "no.such.export;bundle-symbolic-name=\"no.such.provider\";bundle-version=\"[1.1.1,1.1.1]\",no.such.export2;bundle-symbolic-name=\"no.such.provider\";bundle-version=\"[1.1.1,1.1.1]\"";
        String expectedImportsListMixed = "no.such.export;version=\"1.1.1\";bundle-symbolic-name=\"no.such.provider\";bundle-version=\"[1.1.1,1.1.1]\",no.such.export2;bundle-symbolic-name=\"no.such.provider\";bundle-version=\"[1.1.1,1.1.1]\"";
        String expectedImportsListFunkyAttr = "no.such.export;attribute=\"a:=\";bundle-symbolic-name=\"no.such.provider\";bundle-version=\"[1.1.1,1.1.1]\",no.such.export2;attributeTwo=\"b:=\";bundle-symbolic-name=\"no.such.provider\";bundle-version=\"[1.1.1,1.1.1]\"";

        Bundle exportBundle = makeBundleWithExports("no.such.provider",
                "1.1.1", fakeExportsListNoExtras);
        Dictionary headers = makeFragmentFromExportBundle(exportBundle)
                .getHeaders();
        assertEquals(
                "Import list did not match expected value, expectedImportsListNoExtras",
                expectedImportsListNoExtras, headers
                        .get(Constants.IMPORT_PACKAGE));

        exportBundle = makeBundleWithExports("no.such.provider", "1.1.1",
                fakeExportsListAttrOnly);
        headers = makeFragmentFromExportBundle(exportBundle).getHeaders();
        assertEquals(
                "Import list did not match expected value, expectedImportsListAttrOnly",
                expectedImportsListAttrOnly, headers
                        .get(Constants.IMPORT_PACKAGE));

        exportBundle = makeBundleWithExports("no.such.provider", "1.1.1",
                fakeExportsListDirOnly);
        headers = makeFragmentFromExportBundle(exportBundle).getHeaders();
        assertEquals(
                "Import list did not match expected value, expectedImportsListDirOnly",
                expectedImportsListDirOnly, headers
                        .get(Constants.IMPORT_PACKAGE));

        exportBundle = makeBundleWithExports("no.such.provider", "1.1.1",
                fakeExportsListMixed);
        headers = makeFragmentFromExportBundle(exportBundle).getHeaders();
        assertEquals(
                "Import list did not match expected value, expectedImportsListMixed",
                expectedImportsListMixed, headers.get(Constants.IMPORT_PACKAGE));

        exportBundle = makeBundleWithExports("no.such.provider", "1.1.1",
                fakeExportsListFunkyAttr);
        headers = makeFragmentFromExportBundle(exportBundle).getHeaders();
        assertEquals(
                "Import list did not match expected value, expectedImportsListFunkyAttr",
                expectedImportsListFunkyAttr, headers
                        .get(Constants.IMPORT_PACKAGE));
    }

}
