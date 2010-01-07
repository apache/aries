package org.apache.aries.jpa.container.parsing;

import java.io.InputStream;

public interface PersistenceDescriptor {

  /**
   * Get the location of the persistence descriptor
   * @return
   */
  public abstract String getLocation();

  /**
   * Get hold of the wrapped InputStream
   * @return
   */
  public abstract InputStream getInputStream();

}