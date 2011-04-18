package org.apache.aries.blueprint.proxy;

import java.util.concurrent.Callable;

public interface ProxyTestInterface extends Callable<Object> {

  public static final String FIELD = "A Field";
  
  public int doSuff();
}
