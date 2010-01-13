package org.apache.aries.web.converter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.aries.web.converter.WarToWabConverter;

public class WarToWabConverterService implements WarToWabConverter {

  public InputStream convert(InputStreamProvider input, String name, Properties properties) throws IOException {
    WarToWabConverterImpl converter = new WarToWabConverterImpl(input, name, properties);
    return converter.getWAB();
  }

  public Manifest generateManifest(InputStreamProvider input, String name, Properties properties) throws IOException {
    WarToWabConverterImpl converter = new WarToWabConverterImpl(input, name, properties);
    return converter.getWABManifest();
  }

}
