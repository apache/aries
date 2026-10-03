/*
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
package org.apache.aries.util.manifest;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.jar.Manifest;

import org.junit.Test;

public class ManifestProcessorTest {

    private static Manifest parse(String manifest) throws IOException {
        return ManifestProcessor.parseManifest(new ByteArrayInputStream(manifest.getBytes("UTF-8")));
    }

    @Test
    public void testContinuationLineWrappedAfterSpace() throws Exception {
        // Depending on the Java version, a manifest line may be wrapped right after a space
        Manifest m = parse("Manifest-Version: 1.0\r\n"
                + "Import-Package: org.foo; vendor=\"Balcones \r\n"
                + " Fault Software\"\r\n"
                + "\r\n");
        assertEquals("org.foo; vendor=\"Balcones Fault Software\"", m.getMainAttributes().getValue("Import-Package"));
    }

    @Test
    public void testContinuationLineWrappedInsideWord() throws Exception {
        Manifest m = parse("Manifest-Version: 1.0\r\n"
                + "Import-Package: org.foo; vendor=\"Balcone\r\n"
                + " s Fault Software\"\r\n"
                + "\r\n");
        assertEquals("org.foo; vendor=\"Balcones Fault Software\"", m.getMainAttributes().getValue("Import-Package"));
    }

    @Test
    public void testContinuationLineWrappedBeforeSpace() throws Exception {
        Manifest m = parse("Manifest-Version: 1.0\r\n"
                + "Import-Package: org.foo; vendor=\"Balcones\r\n"
                + "  Fault Software\"\r\n"
                + "\r\n");
        assertEquals("org.foo; vendor=\"Balcones Fault Software\"", m.getMainAttributes().getValue("Import-Package"));
    }

    @Test
    public void testValueIsTrimmed() throws Exception {
        Manifest m = parse("Manifest-Version: 1.0  \r\n"
                + "Bundle-Name:   Test  \r\n"
                + "\r\n");
        assertEquals("1.0", m.getMainAttributes().getValue("Manifest-Version"));
        assertEquals("Test", m.getMainAttributes().getValue("Bundle-Name"));
    }
}
