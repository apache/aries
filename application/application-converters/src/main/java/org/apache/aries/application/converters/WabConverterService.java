package org.apache.aries.application.converters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.BundleConverter;
import org.apache.aries.web.converter.WarToWabConverter;
import org.apache.aries.web.converter.WarToWabConverter.InputStreamProvider;

public class WabConverterService implements BundleConverter {
  private WarToWabConverter wabConverter;
  
  public WarToWabConverter getWabConverter() {
    return wabConverter;
  }

  public void setWabConverter(WarToWabConverter wabConverter) {
    this.wabConverter = wabConverter;
  }

  public InputStream convert(IDirectory parentEba, final IFile toBeConverted) {
    try {
      return wabConverter.convert(new InputStreamProvider() {
        public InputStream getInputStream() throws IOException {
          return toBeConverted.open();
        }
      }, toBeConverted.getName(), new Properties());
    } catch (IOException e) {
      // TODO what to do with the Exception
      return null;
    }
  }

}
