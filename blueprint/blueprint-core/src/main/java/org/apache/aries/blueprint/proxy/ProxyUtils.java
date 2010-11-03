package org.apache.aries.blueprint.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ProxyUtils 
{
  public static final Callable<Object> passThrough(final Object target)
  {
    return new Callable<Object>() {
      public Object call() throws Exception {
        return target;
      }
    };
  }
  
  public static final List<Class<?>> asList(Class<?> ... classesArray)
  {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    for (Class<?> clazz : classesArray) {
      classes.add(clazz);
    }
    return classes;
  }
  public static final List<Class<?>> asList(Class<?> clazz)
  {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(clazz);
    return classes;
  }
}