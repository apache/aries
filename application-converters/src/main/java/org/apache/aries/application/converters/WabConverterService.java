package org.apache.aries.application.converters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.BundleConverter;

public class WabConverterService implements BundleConverter {

  public InputStream convert(IDirectory parentEba, IFile toBeConverted) {
    try {
      //TODO find the real name of the WAR file
      WarToWabConverter converter = new WarToWabConverter(toBeConverted, new Properties());
      return converter.getWAB();
    } catch (IOException e) {
      // TODO what to do with the Exception
      return null;
    }
  }

}
