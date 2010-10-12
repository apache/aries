package org.apache.aries.jndi.spi;

import java.util.Hashtable;

public interface EnvironmentAugmentation 
{
  public void augmentEnvironment(Hashtable<?, ?> env);
}