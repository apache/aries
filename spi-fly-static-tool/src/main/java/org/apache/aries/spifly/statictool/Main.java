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
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.aries.spifly.ConsumerHeaderProcessor;
import org.apache.aries.spifly.Streams;
import org.apache.aries.spifly.Util;
import org.apache.aries.spifly.WeavingData;
import org.apache.aries.spifly.api.SpiFlyConstants;
import org.apache.aries.spifly.weaver.TCCLSetterVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class Main {
    private static final String IMPORT_PACKAGE = "Import-Package";

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
        String spiFlyVersion = getMyVersion();
        
        File jarFile = new File(jarPath);
        File tempDir = new File(System.getProperty("java.io.tmpdir") + File.separator + jarFile.getName() + "_" + System.currentTimeMillis());        
        Manifest manifest = unJar(jarFile, tempDir);
        String consumerHeader = manifest.getMainAttributes().getValue(SpiFlyConstants.SPI_CONSUMER_HEADER);
        if (consumerHeader != null) {
            weaveDir(tempDir, consumerHeader);

            manifest.getMainAttributes().remove(new Attributes.Name(SpiFlyConstants.SPI_CONSUMER_HEADER));
            manifest.getMainAttributes().putValue(SpiFlyConstants.PROCESSED_SPI_CONSUMER_HEADER, consumerHeader);
            // TODO if new packages needed then...
            extendImportPackage(spiFlyVersion, manifest);
            
            File newJar = getNewJarFile(jarFile);
            jar(newJar, tempDir, manifest);
        }
        delTree(tempDir);
    }

    private static void extendImportPackage(String spiFlyVersion, Manifest manifest) {
        String ip = manifest.getMainAttributes().getValue(IMPORT_PACKAGE);            
        StringBuilder sb = new StringBuilder(ip);
        sb.append(",");
        sb.append(Util.class.getPackage().getName());
        sb.append(";version=\"[");
        sb.append(spiFlyVersion);
        sb.append(",");
        sb.append(spiFlyVersion);
        sb.append("]\"");
        manifest.getMainAttributes().putValue(IMPORT_PACKAGE, sb.toString());
    }

    private static String getMyVersion() throws IOException {
        // Should be able to leverage the aries.osgi.version file that appears in the target directory here. 
        // Need to figure that out...
        return "0.4.0.SNAPSHOT";
        
//        String classResource = "/" + Main.class.getName().replace(".", "/") + ".class";
//        URL jarUrl = Main.class.getResource(classResource);
//        if (jarUrl != null) {
//            String jarLoc = jarUrl.toExternalForm();
//            Manifest mf = null;
//            if (jarLoc.startsWith("jar:")) {
//                jarLoc.substring("jar:".length());
//                int idx = jarLoc.indexOf("!/");
//                if (idx >= 0) {
//                    jarLoc = jarLoc.substring(0, idx);
//                }
//                
//                JarFile jr = new JarFile(jarLoc);
//                mf = jr.getManifest();
//            } else if (jarLoc.startsWith("file:") && jarLoc.endsWith(classResource)) {
//                String rootDir = jarLoc.substring(0, jarLoc.length() - classResource.length());
//                File manifestFile = new File(rootDir + "/META-INF/MANIFEST.MF");
//                if (manifestFile.exists()) {
//                    mf = new Manifest(new FileInputStream(manifestFile));
//                }
//            }
//            
//            if (mf != null) {
//                String version = mf.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
//                if (version == null)
//                    throw new IOException("Could not obtain the implementation version of this jar file from the manifest");
//                return version.trim();
//            }
//        }
//        throw new IOException("This class can only be executed from inside a jar or an exploded jar file."); 
    }

    private static File getNewJarFile(File jarFile) {
        String s = jarFile.getAbsolutePath();
        int idx = s.lastIndexOf('.');
        s = s.substring(0, idx);
        s += "_spifly.jar";
        return new File(s);
    }

    private static void weaveDir(File dir, String consumerHeader) throws IOException {
        String dirName = dir.getAbsolutePath();
        
        DirTree dt = new DirTree(dir);
        for (File f : dt.getFiles()) {
            if (!f.getName().endsWith(".class"))
                continue;
            
            String className = f.getAbsolutePath().substring(dirName.length());
            if (className.startsWith(File.separator)) 
                className = className.substring(1);
            className = className.substring(0, className.length() - ".class".length());
            className = className.replace(File.separator, ".");
            
            Set<WeavingData> wd = ConsumerHeaderProcessor.processHeader(consumerHeader);
            InputStream is = new FileInputStream(f);
            byte[] b;
            try {
                ClassReader cr = new ClassReader(is);
                ClassWriter cw = new ClassWriter(0);                
                ClassVisitor cv = new TCCLSetterVisitor(cw, className, wd); 
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

    static void delTree(File tempDir) throws IOException {
        for (File f : new DirTree(tempDir).getFiles()) {
            if (!f.delete())
                throw new IOException("Problem deleting file: " + tempDir.getAbsolutePath());
        }
    }

    private static void ensureDirectory(File outDir) throws IOException {
        if (!outDir.isDirectory())
            if (!outDir.mkdirs())
                throw new IOException("Unable to create directory " + outDir.getAbsolutePath());
    }
}

