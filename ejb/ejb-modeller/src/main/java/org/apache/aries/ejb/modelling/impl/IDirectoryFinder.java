package org.apache.aries.ejb.modelling.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.io.IOUtils;
import org.apache.xbean.finder.AbstractFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IDirectoryFinder extends AbstractFinder {

  private static final Logger logger = LoggerFactory.getLogger(IDirectoryFinder.class);
  
  private final List<IDirectory> cpEntries;
  
  private final ClassLoader loader;
  
  public IDirectoryFinder(ClassLoader parent, List<IDirectory> cp) throws ModellerException {
    cpEntries = cp;
    loader = new ResourceClassLoader(parent, cpEntries);
    
    for(IDirectory entry : cpEntries) {
      for(IFile f : entry.listAllFiles()) {
        if(f.getName().endsWith(".class")) {
          try {
            readClassDef(f.open());
          } catch (Exception e) {
            throw new ModellerException(e);
          }
        }
      }
    }
  }
  
  @Override
  protected URL getResource(String arg0) {
    return loader.getResource(arg0);
  }

  @Override
  protected Class<?> loadClass(String arg0) throws ClassNotFoundException {
    return loader.loadClass(arg0);
  }

  
  /**
   * A ClassLoader used by OpenEJB in annotation scanning
   */
  public static class ResourceClassLoader extends ClassLoader {
  
    private final List<IDirectory> classpath;
    public ResourceClassLoader(ClassLoader cl, List<IDirectory> cpEntries) {
      super(cl);
      classpath = cpEntries;
    }
    
    @Override
    protected URL findResource(String resName) {
      for(IDirectory id : classpath) {
        IFile f = id.getFile(resName);
        if(f != null)
          try {
            return f.toURL();
          } catch (MalformedURLException e) {
            logger.error("Error getting URL for file " + f, e);
          }
      }
      return null;
    }
  
    @Override
    protected Class<?> findClass(String className)
        throws ClassNotFoundException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      
      try {
        InputStream is = getResourceAsStream(
                className.replace('.', '/') + ".class");
        if(is == null)
          throw new ClassNotFoundException(className);
        IOUtils.copy(is, baos);
        
        return defineClass(className, baos.toByteArray(), 0, baos.size());
      } catch (IOException e) {
        throw new ClassNotFoundException(className, e);
      }
    }
  }
}
