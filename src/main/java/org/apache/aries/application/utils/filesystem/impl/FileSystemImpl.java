package org.apache.aries.application.utils.filesystem.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.aries.application.filesystem.IDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemImpl {

	  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.application.utils");

	  /**
	   * This method gets the IDirectory that represents the root of a virtual file
	   * system. The provided file can either identify a directory, or a zip file.
	   * 
	   * @param fs the zip file.
	   * @return   the root of the virtual FS.
	   */
	  public static IDirectory getFSRoot(File fs, IDirectory parent)
	  {
	    IDirectory dir = null;
	    
	    if (fs.exists()) {
	      if (fs.isDirectory()) {
	        dir = new DirectoryImpl(fs, fs);
	      } else if (fs.isFile()) {
	        try {
	          dir = new ZipDirectory(fs, fs, parent);
	        } catch (IOException e) {
	          _logger.error ("IOException in IDirectory.getFSRoot", e);
	        }
	      }
	    }
	    else {
	      // since this method does not throw an exception but just returns null, make sure we do not lose the error
	      _logger.error("File not found in IDirectory.getFSRoot", new FileNotFoundException(fs.getPath()));
	    }
	    return dir;
	  }
}
