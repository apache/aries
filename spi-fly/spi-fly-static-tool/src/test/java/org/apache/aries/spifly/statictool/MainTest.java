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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

import org.apache.aries.spifly.Streams;
import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

public class MainTest {
    @Test
    public void testUnJarReJar() throws Exception {
        URL jarURL = getClass().getResource("/testjar.jar");
        File jarFile = new File(jarURL.getFile());
        File tempDir = new File(System.getProperty("java.io.tmpdir") + "/testjar_" + System.currentTimeMillis());
        
        try {
            Manifest manifest = Main.unJar(jarFile, tempDir);
            
            assertStreams(new File(tempDir, "META-INF/MANIFEST.MF"), 
                    "jar:" + jarURL + "!/META-INF/MANIFEST.MF");
            
            assertStreams(new File(tempDir, "A text File with no content"),
                    "jar:" + jarURL + "!/A text File with no content");
            assertStreams(new File(tempDir, "dir/Main.class"),
                    "jar:" + jarURL + "!/dir/Main.class");
            assertStreams(new File(tempDir, "dir/dir 2/a.txt"), 
                    "jar:" + jarURL + "!/dir/dir 2/a.txt");
            assertStreams(new File(tempDir, "dir/dir 2/b.txt"), 
                    "jar:" + jarURL + "!/dir/dir 2/b.txt");
                        
            Assert.assertTrue(new File(tempDir, "dir/dir.3").exists());
            
            // Create a second jar from the exploded directory
            File copiedFile = new File(jarFile.getAbsolutePath() + ".copy");            
            Main.jar(copiedFile, tempDir, manifest);
            URL copyURL = copiedFile.toURI().toURL();
            
            assertStreams("jar:" + copyURL + "!/META-INF/MANIFEST.MF", 
                    "jar:" + jarURL + "!/META-INF/MANIFEST.MF");
            
            assertStreams("jar:" + copyURL + "!/A text File with no content",
                    "jar:" + jarURL + "!/A text File with no content");
            assertStreams("jar:" + copyURL + "!/dir/Main.class",
                    "jar:" + jarURL + "!/dir/Main.class");
            assertStreams("jar:" + copyURL + "!/dir/dir 2/a.txt", 
                    "jar:" + jarURL + "!/dir/dir 2/a.txt");
            assertStreams("jar:" + copyURL + "!/dir/dir 2/b.txt", 
                    "jar:" + jarURL + "!/dir/dir 2/b.txt");
        } finally {
            Main.delTree(tempDir);
        }
    }

    @Test
    public void testDelTree() throws IOException {
        URL jarURL = getClass().getResource("/testjar.jar");
        File jarFile = new File(jarURL.getFile());
        File tempDir = new File(System.getProperty("java.io.tmpdir") + "/testjar_" + System.currentTimeMillis());
        
        assertFalse("Precondition", tempDir.exists());
        Main.unJar(jarFile, tempDir);
        assertTrue(tempDir.exists());
        
        Main.delTree(tempDir);                
        assertFalse(tempDir.exists());
    }
    
    private void assertStreams(String url1, String url2) throws Exception {
        InputStream is1 = new URL(url1).openStream();
        InputStream is2 = new URL(url2).openStream();
        assertStreams(is1, is2);
    }
    
    private void assertStreams(File file, String url) throws Exception {
        InputStream is1 = new FileInputStream(file);
        InputStream is2 = new URL(url).openStream();
        assertStreams(is1, is2);
    }

    private void assertStreams(InputStream is1, InputStream is2)
            throws IOException, ArrayComparisonFailure {
        try {
            byte[] bytes1 = Streams.suck(is1);
            byte[] bytes2 = Streams.suck(is2);
            Assert.assertArrayEquals("Files not equal", bytes1, bytes2);
        } finally {
            is1.close();
            is2.close();
        }
    }
}


