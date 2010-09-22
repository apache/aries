package org.apache.aries.jndi.urls;

import javax.naming.spi.ObjectFactory;

public interface URLObjectFactoryFinder 
{
  public ObjectFactory findFactory(String url);
}
