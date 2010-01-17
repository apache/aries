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
package org.apache.aries.unittest.fixture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.osgi.framework.Constants;

/**
 * Utility class for creating archive-based fixtures such as EBA archives, jar files etc.
 * This class provides a flow based api for defining such fixtures. For example, a simple EBA archive could 
 * be defined as such:
 * 
 * <code>
 * ArchiveFixtures.ZipFixture zip = ArchiveFixtures.newZip()
 *   .jar("test.jar")
 *     .manifest()
 *       .symbolicName("com.ibm.test")
 *       .version("2.0.0")
 *     .end()
 *     .file("random.txt", "Some text")
 *   .end();
 * </code>
 * 
 * This defines a zip archive containing a single jar file (hence no application manifest). The jar file itself has
 * a manifest and a text file.
 * 
 * To actually create the physical archive use the <code>writeOut</code> method on the archive fixture.
 */
public class ArchiveFixture
{
  /**
   * Create a new zip file fixture
   * @return
   */
  public static ZipFixture newZip() {
    return new ZipFixture(null);
  }
  
  /**
   * Create a new jar file fixture
   * @return
   */
  public static JarFixture newJar() {
    return new JarFixture(null);
  }
  
  /**
   * Utility to copy an InputStream into an OutputStream. Closes the InputStream afterwards.
   * @param in
   * @param out
   * @throws IOException
   */
  private static void copy(InputStream in, OutputStream out) throws IOException
  {
    try {
      int len;
      byte[] b = new byte[1024];
      while ((len = in.read(b)) != -1)
        out.write(b,0,len);
    }
    finally {
      in.close();
    }
  }

  /**
   * Base interface for every fixture.
   */
  public interface Fixture {
    /**
     * Write the physical representation of the fixture to the given OutputStream
     * @param out
     * @throws IOException
     */
    void writeOut(OutputStream out) throws IOException;
  }
  
  /**
   * Abstract base class for fixtures. Archive fixtures are by nature hierarchical.
   */
  public static abstract class AbstractFixture implements Fixture {
    private ZipFixture parent;
    
    protected AbstractFixture(ZipFixture parent) {
      this.parent = parent;
    }
    
    /**
     * Ends the current flow target and returns the parent flow target. For example, in the
     * following code snippet the <code>end</code> after <code>.version("2.0.0")</code> marks
     * the end of the manifest. Commands after that relate to the parent jar file of the manifest.
     * 
     * <code>
     * ArchiveFixtures.ZipFixture zip = ArchiveFixtures.newZip()
     *   .jar("test.jar")
     *     .manifest()
     *       .symbolicName("com.ibm.test")
     *       .version("2.0.0")
     *     .end()
     *     .file("random.txt", "Some text")
     *   .end();
     * </code>
     * @return
     */
    public ZipFixture end() {
      return parent;
    }
  }

  /**
   * Simple fixture for text files.
   */
  public static class FileFixture extends AbstractFixture {
    private StringBuffer text = new StringBuffer();
    
    protected FileFixture(ZipFixture parent) {
      super(parent);
    }
    
    /**
     * Add a line to the file fixture. The EOL character is added automatically.
     * @param line
     * @return
     */
    public FileFixture line(String line) {
      text.append(line);
      text.append("\n");
      return this;
    }
    
    public void writeOut(OutputStream out) throws IOException {
      out.write(text.toString().getBytes());
    }
  }
  
  public static class IStreamFixture extends AbstractFixture {
    private byte[] bytes;
    
    protected IStreamFixture(ZipFixture parent, InputStream input) throws IOException {
      super(parent);

      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try {
        copy(input, output);
      } finally {
        output.close();
      }
      
      bytes = output.toByteArray();
    }

    public void writeOut(OutputStream out) throws IOException {
      copy(new ByteArrayInputStream(bytes), out);
    }
  }
  
  /**
   * Fixture for (bundle) manifests. By default, they contain the lines
   * 
   * <code>
   * Manifest-Version: 1
   * Bundle-ManifestVersion: 2
   * </code>
   */
  public static class ManifestFixture extends AbstractFixture {
    private Manifest mf;
    
    protected Manifest getManifest()
    {
      return mf;
    }
    
    protected ManifestFixture(ZipFixture parent) {
      super(parent);
      mf = new Manifest();
      mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1");
      mf.getMainAttributes().putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
    }
    
    /**
     * Set the symbolic name of the bundle
     * @param name
     * @return
     */
    public ManifestFixture symbolicName(String name)
    {
      mf.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, name);
      return this;
    }
    
    /**
     * Set the version of the bundle
     * @param version
     * @return
     */
    public ManifestFixture version(String version)
    {
      mf.getMainAttributes().putValue(Constants.BUNDLE_VERSION, version);
      return this;
    }
    
    /**
     * Add a custom attribute to the manifest. Use the more specific methods for symbolic name and version.
     * @param name
     * @param value
     * @return
     */
    public ManifestFixture attribute(String name, String value)
    {
      mf.getMainAttributes().putValue(name, value);
      return this;
    }
    
    public void writeOut(OutputStream out) throws IOException
    {
      mf.write(out);
    }
  }

  /**
   * Fixture for a jar archive. It offers the same functionality as zip fixtures.
   * The main difference is that in a jar archive the manifest will be output as the first file,
   * regardless of when it is added.
   */
  public static class JarFixture extends ZipFixture {
    private ManifestFixture mfFixture;
    
    protected JarFixture(ZipFixture parent) {
      super(parent);
    }
    
    @Override
    public ManifestFixture manifest()
    {
      if (mfFixture != null)
        throw new IllegalStateException("Only one manifest allowed, you dummy ;)");
      
      mfFixture = new ManifestFixture(this);
      return mfFixture;
    }
    
    @Override
    public void writeOut(OutputStream out) throws IOException
    {
      if (bytes == null) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        JarOutputStream jout;
        if (mfFixture != null)
          jout = new JarOutputStream(bout, mfFixture.getManifest());
        else
          jout = new JarOutputStream(bout);
        
        try {
          writeAllEntries(jout);
        } finally {
          jout.close();
        }
        
        bytes = bout.toByteArray();
      }
      
      ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
      copy(bin, out);
    }
  }
  
  /**
   * Base fixture for any kind of zip archive. Zip archives can contain any number of child archives 
   * given by an archive type and a path. The order in which these child archives are added is important
   * because it will be the order in which they are added to the zip.
   */
  public static class ZipFixture extends AbstractFixture {
    protected static class ChildFixture {
      public String path;
      public Fixture fixture;
      
      public ChildFixture(String path, Fixture fixture)
      {
        this.path = path;
        this.fixture = fixture;
      }
    }
    
    protected List<ChildFixture> children = new ArrayList<ChildFixture>();
    protected byte[] bytes = null;
    
    protected ZipFixture(ZipFixture parent) {
      super(parent);
    }
        
    /**
     * Create a child zip fixture at the given target.
     * @param path
     * @return
     */
    public ZipFixture zip(String path) {
      ZipFixture res = new ZipFixture(this);
      children.add(new ChildFixture(path, res));
      
      return res;
    }
    
    /**
     * Create a child jar fixture at the given path.
     * @param path
     * @return
     */
    public ZipFixture jar(String path) {
      JarFixture res = new JarFixture(this);
      children.add(new ChildFixture(path, res));
      
      return res;
    }
    
    /**
     * Create a complete child file fixture at the given path and with the content.
     * Note: this will return the current zip fixture and not the file fixture.
     * 
     * @param path
     * @param content
     * @return
     */
    public ZipFixture file(String path, String content) 
    {
      return file(path).line(content).end();
    }
    
    /**
     * Create an empty file fixture at the given path.
     * 
     * @param path
     * @return
     */
    public FileFixture file(String path)
    {
      FileFixture res = new FileFixture(this);
      children.add(new ChildFixture(path, res));
      
      return res;
    }
    
    /**
     * Create a binary file with the content from the input stream
     * @param path
     * @param input
     * @return
     */
    public ZipFixture binary(String path, InputStream input) throws IOException {
      IStreamFixture child = new IStreamFixture(this, input);
      children.add(new ChildFixture(path, child));
      
      return this;
    }
    
    /**
     * Create a binary file that is populated from content on the classloader
     * @param path
     * @param resourcePath Path that the resource can be found in the current classloader
     * @return
     */
    public ZipFixture binary(String path, String resourcePath) throws IOException {
      return binary(path, getClass().getClassLoader().getResourceAsStream(resourcePath));
    }
    
    /**
     * Create a manifest fixture at the given path.
     * @return
     */
    public ManifestFixture manifest()
    {
      ManifestFixture res = new ManifestFixture(this);
      children.add(new ChildFixture("META-INF/MANIFEST.MF", res));
      
      return res;
    }
    
    /**
     * Ensure that the necessary directory entries for the entry are available
     * in the zip file. Newly created entries are added to the set of directories.
     * 
     * @param zout
     * @param entry
     * @param existingDirs
     * @throws IOException
     */
    private void mkDirs(ZipOutputStream zout, String entry, Set<String> existingDirs) throws IOException
    {
      String[] parts = entry.split("/");
      String dirName = "";
      for (int i=0;i<parts.length-1;i++) {
        dirName += parts[i] + "/";
        if (!!!existingDirs.contains(dirName)) {
          ZipEntry ze = new ZipEntry(dirName);
          zout.putNextEntry(ze);
          zout.closeEntry();
          
          existingDirs.add(dirName);
        }
      }
    }
    
    /**
     * Add all entries to the ZipOutputStream
     * @param zout
     * @throws IOException
     */
    protected void writeAllEntries(ZipOutputStream zout) throws IOException
    {
      Set<String> dirs = new HashSet<String>();
      
      for (ChildFixture child : children) {
        mkDirs(zout, child.path, dirs);
        
        ZipEntry ze = new ZipEntry(child.path);
        zout.putNextEntry(ze);
        child.fixture.writeOut(zout);
        zout.closeEntry();
      }      
    }
    
    public void writeOut(OutputStream out) throws IOException 
    {
      /*
       * For better reuse this method delegate the writing to writeAllEntries, which
       * can be reused by the JarFixture.
       */
      if (bytes == null) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(bout);
        try {
          writeAllEntries(zout);
        } finally {
          zout.close();
        }
        
        bytes = bout.toByteArray();
      }

      ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
      copy(bin, out);
    }
  }

}
