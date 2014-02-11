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
package org.apache.aries.spifly.statictool;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.aries.spifly.SpiFlyConstants;
import org.apache.aries.spifly.Streams;
import org.apache.aries.spifly.statictool.bundle.Test2Class;
import org.apache.aries.spifly.statictool.bundle.Test3Class;
import org.apache.aries.spifly.statictool.bundle.TestClass;
import org.junit.Assert;
import org.junit.Test;

public class RequirementTest {
    @Test
    public void testConsumerBundle() throws Exception {
        String testClassFileName = TestClass.class.getName().replace('.', '/') + ".class";
        URL testClassURL = getClass().getResource("/" + testClassFileName);
        String test2ClassFileName = Test2Class.class.getName().replace('.', '/') + ".class";
        URL test2ClassURL = getClass().getResource("/" + test2ClassFileName);
        String test3ClassFileName = Test3Class.class.getName().replace('.', '/') + ".class";
        URL test3ClassURL = getClass().getResource("/" + test3ClassFileName);

        File jarFile = new File(System.getProperty("java.io.tmpdir") + "/testjar_" + System.currentTimeMillis() + ".jar");
        File expectedFile = null;
        try {
            // Create the jarfile to be used for testing
            Manifest mf = new Manifest();
            Attributes mainAttributes = mf.getMainAttributes();
            mainAttributes.putValue("Manifest-Version", "1.0");
            mainAttributes.putValue("Bundle-ManifestVersion", "2.0");
            mainAttributes.putValue("Bundle-SymbolicName", "testbundle");
            mainAttributes.putValue("Foo", "Bar Bar");
            mainAttributes.putValue("Import-Package", "org.foo.bar");
            mainAttributes.putValue(SpiFlyConstants.REQUIRE_CAPABILITY,
                    "osgi.serviceloader; filter:=\"(osgi.serviceloader=org.apache.aries.spifly.mysvc.SPIProvider)\";cardinality:=multiple, " +
                    "osgi.extender; filter:=\"(osgi.extender=osgi.serviceloader.processor)\"");

            JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), mf);
            jos.putNextEntry(new ZipEntry(testClassFileName));
            Streams.pump(testClassURL.openStream(), jos);
            jos.putNextEntry(new ZipEntry(test2ClassFileName));
            Streams.pump(test2ClassURL.openStream(), jos);
            jos.putNextEntry(new ZipEntry(test3ClassFileName));
            Streams.pump(test3ClassURL.openStream(), jos);
            jos.close();

            Main.main(jarFile.getCanonicalPath());

            expectedFile = new File(jarFile.getParent(), jarFile.getName().replaceAll("[.]jar", "_spifly.jar"));
            Assert.assertTrue("A processed separate bundle should have been created", expectedFile.exists());
            // Check manifest in generated bundle.
            JarFile transformedJarFile = new JarFile(expectedFile);
            Manifest actualMF = transformedJarFile.getManifest();
            Assert.assertEquals("1.0", actualMF.getMainAttributes().getValue("Manifest-Version"));
            Assert.assertEquals("2.0", actualMF.getMainAttributes().getValue("Bundle-ManifestVersion"));
            Assert.assertEquals("testbundle", actualMF.getMainAttributes().getValue("Bundle-SymbolicName"));
            Assert.assertEquals("Bar Bar", actualMF.getMainAttributes().getValue("Foo"));
            Assert.assertEquals("osgi.serviceloader; filter:=\"(osgi.serviceloader=org.apache.aries.spifly.mysvc.SPIProvider)\";cardinality:=multiple",
                    actualMF.getMainAttributes().getValue(SpiFlyConstants.REQUIRE_CAPABILITY));
            Assert.assertNull("Should not generate this header when processing Require-Capability",
                    actualMF.getMainAttributes().getValue(SpiFlyConstants.PROCESSED_SPI_CONSUMER_HEADER));
            String importPackage = actualMF.getMainAttributes().getValue("Import-Package");
            Assert.assertTrue(
                "org.foo.bar,org.apache.aries.spifly;version=\"[1.0.0,1.1.0)\"".equals(importPackage) ||
                "org.apache.aries.spifly;version=\"[1.0.0,1.1.0)\",org.foo.bar".equals(importPackage));

            JarFile initialJarFile = new JarFile(jarFile);
            byte[] orgBytes = Streams.suck(initialJarFile.getInputStream(new ZipEntry(testClassFileName)));
            byte[] nonTransBytes = Streams.suck(transformedJarFile.getInputStream(new ZipEntry(testClassFileName)));
            Assert.assertArrayEquals(orgBytes, nonTransBytes);

            byte[] orgBytes2 = Streams.suck(initialJarFile.getInputStream(new ZipEntry(test2ClassFileName)));
            byte[] nonTransBytes2 = Streams.suck(transformedJarFile.getInputStream(new ZipEntry(test2ClassFileName)));
            Assert.assertArrayEquals(orgBytes2, nonTransBytes2);

            byte[] orgBytes3 = Streams.suck(initialJarFile.getInputStream(new ZipEntry(test3ClassFileName)));
            byte[] transBytes3 = Streams.suck(transformedJarFile.getInputStream(new ZipEntry(test3ClassFileName)));
            Assert.assertFalse("The transformed class should be different", Arrays.equals(orgBytes3, transBytes3));

            initialJarFile.close();
            transformedJarFile.close();
        } finally {
            jarFile.delete();

            if (expectedFile != null)
                expectedFile.delete();
        }
    }
}
