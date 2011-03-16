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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.aries.spifly.ConsumerHeaderProcessor;
import org.apache.aries.spifly.Streams;
import org.apache.aries.spifly.TCCLSetterVisitor;
import org.apache.aries.spifly.WeavingData;
import org.apache.aries.spifly.api.SpiFlyConstants;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class Main {
    public static void usage() {
        System.err.println();
        System.err.println("Usage: java " + Main.class.getName() + " bundle1.jar bundle2.jar ...");
        System.exit(-1);
    }
    
    public static void main(String ... args) throws Exception {
        for (String arg : args) {
            weaveJar(arg);
        }
    }

    private static void weaveJar(String jarPath) throws IOException {
        File jarFile = new File(jarPath);
        File tempDir = new File(System.getProperty("java.io.tmpdir") + File.separator + jarFile.getName() + "_" + System.currentTimeMillis());        
        Manifest manifest = unJar(jarFile, tempDir);
        String consumerHeader = manifest.getMainAttributes().getValue(SpiFlyConstants.SPI_CONSUMER_HEADER);
        if (consumerHeader != null) {
            weaveDir(tempDir, consumerHeader);
            // jar(tempDir, newJar);
        }
        // finally - clean up
    }

    private static void weaveDir(File dir, String consumerHeader) throws IOException {
        DirTree dt = new DirTree(dir);
        for (File f : dt.getFiles()) {
            if (!f.getName().endsWith(".class"))
                continue;
            
            WeavingData[] wd = ConsumerHeaderProcessor.processHeader(consumerHeader);
            InputStream is = new FileInputStream(f);
            byte[] b;
            try {
                ClassReader cr = new ClassReader(is);
                ClassWriter cw = new ClassWriter(0);
                ClassVisitor cv = new TCCLSetterVisitor(cw, null, wd); 
                cr.accept(cv, 0);
                b = cw.toByteArray();
            } finally {
                is.close();
            }
            
            OutputStream os = new FileOutputStream(f);
            try {
                os.write(b);                
            } finally {
                os.close();
            }
        }
    }

    static Manifest unJar(File jarFile, File tempDir) throws IOException {
        ensureDirectory(tempDir);

        JarInputStream jis = new JarInputStream(new FileInputStream(jarFile));
        JarEntry je = null;
        while((je = jis.getNextJarEntry()) != null) {
            if (je.isDirectory()) {
                File outDir = new File(tempDir, je.getName());
                ensureDirectory(outDir);
                
                continue;
            }
            
            File outFile = new File(tempDir, je.getName());
            File outDir = outFile.getParentFile();
            ensureDirectory(outDir); 
            
            OutputStream out = new FileOutputStream(outFile);
            try {
                Streams.pump(jis, out);
            } finally {
                out.flush();
                out.close();
                jis.closeEntry();
            }
            outFile.setLastModified(je.getTime());
        }
        
        Manifest manifest = jis.getManifest();
        if (manifest != null) {
            File mf = new File(tempDir, "META-INF/MANIFEST.MF");
            File mfDir = mf.getParentFile();
            ensureDirectory(mfDir);

            OutputStream out = new FileOutputStream(mf);
            try {
                manifest.write(out);
            } finally {
                out.flush();
                out.close();                
            }
        }
        
        jis.close();
        return manifest;
    }
    
    static void jar(File jarFile, File rootFile, Manifest manifest) throws IOException {
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest);
        try {
            addToJarRecursively(jos, rootFile.getAbsoluteFile(), rootFile.getAbsolutePath());
        } finally {
            jos.close();
        }
    }

    static void addToJarRecursively(JarOutputStream jar, File source, String rootDirectory) throws IOException {                
        String sourceName = source.getAbsolutePath().replace("\\", "/");
        sourceName = sourceName.substring(rootDirectory.length());
        
        if (sourceName.startsWith("/")) {
            sourceName = sourceName.substring(1);
        }
        
        if ("META-INF/MANIFEST.MF".equals(sourceName))
            return;
        
        if (source.isDirectory()) {
            /* Is there any point in adding a directory beyond just taking up space? 
            if (!sourceName.isEmpty()) {
                if (!sourceName.endsWith("/")) {
                    sourceName += "/";                        
                }
                JarEntry entry = new JarEntry(sourceName);
                jar.putNextEntry(entry);
                jar.closeEntry();
            }
            */
            for (File nested : source.listFiles()) {
                addToJarRecursively(jar, nested, rootDirectory);
            }
            return;
        }
        
        JarEntry entry = new JarEntry(sourceName);
        jar.putNextEntry(entry);
        InputStream is = new FileInputStream(source);
        try {
            Streams.pump(is, jar);
        } finally {
            jar.closeEntry();
            is.close();
        }
    }

    private static void ensureDirectory(File outDir) throws IOException {
        if (!outDir.isDirectory())
            if (!outDir.mkdirs())
                throw new IOException("Unable to create directory " + outDir.getAbsolutePath());
    }
}

