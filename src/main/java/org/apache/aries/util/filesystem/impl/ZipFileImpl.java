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

package org.apache.aries.util.filesystem.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of IFile that represents a file entry in a zip.
 */
public class ZipFileImpl implements IFile
{
  /** A logger */
  private static final Logger logger = LoggerFactory.getLogger(ZipFileImpl.class.getName());

  /** The name of the file */
  private String name;
  /** The size of the file */
  private final long size;
  /** The last time the file was updated */
  private final long lastModified;
  /** The zip file this is contained in */
  protected final File zip;
  /** The entry in the zip this IFile represents */
  protected final ZipEntry entry;
  /** The parent directory */
  private final IDirectory parent;
  /** The URL of the zip file we are looking inside of */
  private final String url;
  /** The path of the zip archive to the VFS root */
  private final String zipPathToRoot;
  /** The closeable directory that caches the open ZipFile */
  protected final ZipCloseableDirectory cache;
  
  /**
   * This constructor is used to create a file entry within the zip.
   * 
   * @param zip1    the zip file the entry is in.
   * @param entry1  the entry this IFile represents.
   * @param parent1 the parent directory.
   */
  public ZipFileImpl(File zip1, ZipEntry entry1, ZipDirectory parent1, ZipCloseableDirectory cache)
  {
    this.zip = zip1;
    this.entry = entry1;
    
    this.zipPathToRoot = parent1.getZipPathToRoot();

    name = zipPathToRoot + entry1.getName();
    
    if (entry1.isDirectory()) name = name.substring(0, name.length() - 1);
    
    lastModified = entry1.getTime();
    size = entry1.getSize();
    
    url = ((ZipFileImpl)parent1).url;
    
    this.parent = parent1;
    this.cache = cache;
  }
  
  /**
   * This is called to construct the root directory of the zip.
   * 
   * @param zip1 the zip file this represents.
   * @param fs   the file on the fs.
   * @param rootName the name of this zipfile relative to the IFile filesystem root
   * @throws MalformedURLException
   */
  protected ZipFileImpl(File zip1, IDirectory parent) throws MalformedURLException
  {
    this.zip = zip1;
    this.entry = null;
    
    if (parent == null) {
        name = "";
        zipPathToRoot = "";
        this.parent = null;
    } else {
    	this.parent = parent;
    	name = parent.getName() + "/" + zip1.getName();
    	zipPathToRoot = name+"/";
    }
    
    lastModified = zip1.lastModified();
    size = zip1.length();
    url = zip1.toURI().toURL().toExternalForm();
    this.cache = null;
  }
  
  public ZipFileImpl(ZipFileImpl other, ZipCloseableDirectory cache) {
	  name = other.name;
	  size = other.size;
	  lastModified = other.lastModified;
	  zip = other.zip;
	  entry = other.entry;
	  parent = other.parent;
	  url = other.url;
	  zipPathToRoot = other.zipPathToRoot;
	  this.cache = cache;
  }

  /**
   * Obtain the path of the zip file to the VFS root
   */
  public String getZipPathToRoot() {
	  return zipPathToRoot;
  }
  
  @Override
  public IDirectory convert()
  {
    return null;
  }
  
  @Override
  public IDirectory convertNested() {
	  if (isDirectory()) return convert();
	  else if (FileSystemImpl.isValidZip(this)) return new NestedZipDirectory(this); 
	  else return null;
  }	

  @Override
  public long getLastModified()
  {
    return lastModified;
  }

  @Override
  public String getName()
  {
    return name;
  }
  
  public String getNameInZip() 
  {
	  if (entry == null) return "";
	  else {
		  String name = entry.getName();
		  if (isDirectory()) return name.substring(0, name.length()-1);
		  else return name;
	  }
  }

  @Override
  public IDirectory getParent()
  {
    return parent;
  }

  @Override
  public long getSize()
  {
    return size;
  }

  @Override
  public boolean isDirectory()
  {
    return false;
  }

  @Override
  public boolean isFile()
  {
    return true;
  }

  @Override
  public InputStream open() throws IOException
  {
    InputStream is = new SpecialZipInputStream(entry);
    return is;
  }
  
  @Override
  public IDirectory getRoot()
  {
    return parent.getRoot();
  }

  @Override
  public URL toURL() throws MalformedURLException
  {
    String entryURL = "jar:" + url + "!/" + entry.getName();
    URL result = new URL(entryURL);
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null) return false;
    if (obj == this) return true;
    
    if (obj.getClass() == getClass()) {
      return toString().equals(obj.toString());
    }
    
    return false;
  }

  @Override
  public int hashCode()
  {
    return toString().hashCode();
  }

  @Override
  public String toString()
  {
	  if (name != null && name.length() != 0) return url.substring(5)+ "/" + name;
	  else return url.substring(5);
  }
  
  ZipFile openZipFile(){
    ZipFile z = null;

    if (cache != null && !!!cache.isClosed()) {
    	z = cache.getZipFile();
    } else {
	    try {
	      z = new ZipFile(zip);
	    } catch (ZipException e) {
	      logger.error ("ZipException in ZipFileImpl.openZipFile", e);
	    } catch (IOException e) {
	      logger.error ("IOException in ZipFileImpl.openZipFile", e);
	    }
    }
    return z;
  }
  
  void closeZipFile(ZipFile z){
	  if (cache != null && cache.getZipFile() == z) {
		  // do nothing
	  } else {
		  try{
			  z.close();
		  }
		  catch (IOException e) {
			  logger.error ("IOException in ZipFileImpl.closeZipFile", e);
		  }
	  }
  }
  
  /**
   * A simple class to delegate to the InputStream of the constructor
   * and to call close on the zipFile when we close the stream.
   *
   */
  private class SpecialZipInputStream extends InputStream{

    private ZipFile zipFile;
    private InputStream is;
    
    public SpecialZipInputStream(ZipEntry anEntry){
      try{
      this.zipFile = openZipFile();
      this.is = zipFile.getInputStream(anEntry);
      }
      catch (ZipException e) {
        logger.error ("ZipException in SpecialZipInputStream()", e);
      } catch (IOException e) {
        logger.error ("IOException in SpecialZipInputStream()", e);        
      }
    }
    
    @Override
    public int read() throws IOException
    {
      return is.read();
    }
    
    @Override
    public void close() throws IOException{
        //call close on the input stream, probably does nothing
        is.close();
        //call close on the zip file, important for tidying up
        closeZipFile(zipFile);
    }
    
  }

}
