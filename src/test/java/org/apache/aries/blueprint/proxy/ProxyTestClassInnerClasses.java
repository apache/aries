package org.apache.aries.blueprint.proxy;

import java.lang.reflect.Proxy;

public class ProxyTestClassInnerClasses {

  public static class ProxyTestClassStaticInner {
    public String sayHello() {
      return "Hello";
    }
  }
  
  public class ProxyTestClassInner {
    
    public String sayGoodbye() {
      return "Goodbye";
    }
  }
  
  public class ProxyTestClassUnweavableInnerParent {
    
    public ProxyTestClassUnweavableInnerParent(int i) {
      
    }
    
    public String wave() {
      return "Wave";
    }
  }
  
  public class ProxyTestClassUnweavableInnerChild extends 
       ProxyTestClassUnweavableInnerParent {
    
      public ProxyTestClassUnweavableInnerChild() {
        super(1);
      }
      public ProxyTestClassUnweavableInnerChild(int i) {
        super(i);
      }

      public String leave() {
        return "Gone";
      }
  }
}
