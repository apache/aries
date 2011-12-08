package org.apache.aries.blueprint.proxy;

import java.util.concurrent.Callable;

public class ProxyTestClassChildOfAbstract extends ProxyTestClassAbstract implements Callable<String>{

  @Override
  public String getMessage() {
    return "Working";
  }

  public String call() throws Exception {
    return "Callable Works too!";
  }

}
