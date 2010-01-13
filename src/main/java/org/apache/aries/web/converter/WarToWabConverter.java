package org.apache.aries.web.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Manifest;

public interface WarToWabConverter {
  public static interface InputStreamProvider {
    InputStream getInputStream() throws IOException;
  }

  /**
   * Generate the new manifest for the 
   * @param input
   * @param name The name of the war file
   * @param properties Properties to influence the conversion as defined in RFC66
   * @return
   */
  Manifest generateManifest(InputStreamProvider input, String name, Properties properties) throws IOException;
  
  /**
   * Generate the converter WAB file. This file includes all the files from the input
   * and has the new manifest.
   * @param input
   * @param name The name of the war file
   * @param properties Properties to influence the conversion as defined in RFC66
   * @return
   */
  InputStream convert(InputStreamProvider input, String name, Properties properties) throws IOException;
}
