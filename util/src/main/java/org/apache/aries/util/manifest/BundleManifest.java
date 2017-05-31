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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.util.manifest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.aries.util.IORuntimeException;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.internal.MessageUtil;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Entity class to retrieve and represent a bundle manifest (valid or invalid).
 */
public class BundleManifest
{
  private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

  /**
   * Read a manifest from a jar input stream. This will find the manifest even if it is NOT
   * the first file in the archive.
   *
   * @param is the jar input stream
   * @return the bundle manifest
   */
  public static BundleManifest fromBundle(InputStream is) {
    JarInputStream jarIs = null;
    try {
      jarIs = new JarInputStream(is);
      Manifest m = jarIs.getManifest();
      if (m != null)
        return new BundleManifest(m);
      else {
        ZipEntry entry;
        while ((entry = jarIs.getNextEntry()) != null) {
          if (entry.getName().equals(MANIFEST_PATH))
            return new BundleManifest(jarIs);
        }

        return null;
      }
    }
    catch (IOException e) {
      throw new IORuntimeException("IOException in BundleManifest()", e);
    }
    finally {
      IOUtils.close(jarIs);
    }
  }

  /**
   * Retrieve a BundleManifest from the given jar file
   *
   * @param f the bundle jar file
   * @return the bundle manifest
   */
  public static BundleManifest fromBundle(IFile f) {
    InputStream is = null;
    try {
      if (f.isDirectory()) {
        IFile manFile = f.convert().getFile(MANIFEST_PATH);
        if (manFile != null)
          return new BundleManifest(manFile.open());
        else
          return null;
      } else {
        is = f.open();
        return fromBundle(is);
      }
    } catch (IOException e) {
      throw new IORuntimeException("IOException in BundleManifest.fromBundle(IFile)", e);
    }
    finally {
      IOUtils.close(is);
    }
  }

  /**
   * Retrieve a bundle manifest from the given jar file, which can be exploded or compressed
   *
   * @param f the bundle jar file
   * @return the bundle manifest
   */
  public static BundleManifest fromBundle(File f) {
    if (f.isDirectory()) {
      File manifestFile = new File(f, MANIFEST_PATH);
      if (manifestFile.isFile())
        try {
          return new BundleManifest(new FileInputStream(manifestFile));
        }
        catch (IOException e) {
          throw new IORuntimeException("IOException in BundleManifest.fromBundle(File)", e);
        }
      else
        return null;
    }
    else  if (f.isFile()) {
      try {
        return fromBundle(new FileInputStream(f));
      }
      catch (IOException e) {
        throw new IORuntimeException("IOException in BundleManifest.fromBundle(File)", e);
      }
    }
    else {
      throw new IllegalArgumentException(MessageUtil.getMessage("UTIL0016E", f.getAbsolutePath()));
    }
  }

  private Manifest manifest;

  /**
   * Create a BundleManifest object from the InputStream to the manifest (not to the bundle)
   * @param manifestIs
   * @throws IOException
   */
  public BundleManifest(InputStream manifestIs) throws IOException {
    this(ManifestProcessor.parseManifest(manifestIs));
  }

  /**
   * Create a BundleManifest object from a common Manifest object
   * @param m
   */
  public BundleManifest(Manifest m) {
    manifest = m;
  }

  public String getSymbolicName() {
    String rawSymName = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);

    String result = null;
    if (rawSymName != null) {
      NameValuePair info = ManifestHeaderProcessor.parseBundleSymbolicName(rawSymName);
      result = info.getName();
    }

    return result;
  }

  public Version getVersion() {
    String specifiedVersion = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
    Version result = (specifiedVersion == null) ? Version.emptyVersion : new Version(specifiedVersion);

    return result;
  }

  public String getManifestVersion() {
    return manifest.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION);
  }

  public Attributes getRawAttributes() {
    return manifest.getMainAttributes();
  }

  public Manifest getRawManifest() {
    return manifest;
  }

  public boolean isValid() {
    return getManifestVersion() != null && getSymbolicName() != null;
  }
}

