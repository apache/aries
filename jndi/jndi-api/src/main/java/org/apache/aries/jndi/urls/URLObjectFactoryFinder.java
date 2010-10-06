package org.apache.aries.jndi.urls;

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

public interface URLObjectFactoryFinder 
{
  public ObjectFactory findFactory(String url, Hashtable<?, ?> env) throws NamingException;
}
