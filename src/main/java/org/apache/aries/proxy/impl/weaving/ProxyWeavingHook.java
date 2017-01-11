/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.proxy.impl.weaving;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.weaving.WovenProxy;
import org.apache.aries.proxy.weavinghook.ProxyWeavingController;
import org.apache.aries.proxy.weavinghook.WeavingHelper;
import org.objectweb.asm.ClassReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProxyWeavingHook implements WeavingHook, WeavingHelper {

  public static final String WEAVING_ENABLED_CLASSES = "org.apache.aries.proxy.weaving.enabled";
  public static final String WEAVING_DISABLED_CLASSES = "org.apache.aries.proxy.weaving.disabled";

  public static final String WEAVING_ENABLED_CLASSES_DEFAULT = "*";
  public static final String WEAVING_DISABLED_CLASSES_DEFAULT = "org.objectweb.asm.*,org.slf4j.*,org.apache.log4j.*,javax.*,ch.qos.logback.*";

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyWeavingHook.class);
  /** An import of the WovenProxy package */
  private static final String IMPORT_A = "org.apache.aries.proxy.weaving";
  /** 
   * An import for the InvocationListener class that we will need.
   * This should automatically wire to the right thing because of the uses clause
   * on the impl.weaving package
   */
  private static final String IMPORT_B = "org.apache.aries.proxy";

  private final List<Pattern> enabled;
  private final List<Pattern> disabled;
  @SuppressWarnings("rawtypes")
  private final ServiceTracker controllers;

  @SuppressWarnings({
 "unchecked", "rawtypes"
})
public ProxyWeavingHook(BundleContext context) {
    String enabledProp = context != null ? context.getProperty(WEAVING_ENABLED_CLASSES) : null;
    enabled = parseMatchers(enabledProp, WEAVING_ENABLED_CLASSES_DEFAULT);
    disabled = parseMatchers(context != null ? context.getProperty(WEAVING_DISABLED_CLASSES) : null, WEAVING_DISABLED_CLASSES_DEFAULT);
    controllers = new ServiceTracker(context, ProxyWeavingController.class.getName(), null);
    controllers.open();
    
    if (!"none".equals(enabledProp)) {
        Dictionary<String,String> props = new Hashtable<String,String>();
        // SubsystemResource.java also uses this constant. 
        //   While it could be turned into a static final constant, note that this
        //   is also a non-standard workaround in the absence of a solution in the spec. 
        // See the associated OSGi spec bug. 
        props.put("osgi.woven.packages", "org.apache.aries.proxy.weaving,org.apache.aries.proxy");
        context.registerService("org.osgi.framework.hooks.weaving.WeavingHook", this, props);
    }
  }

  public final void weave(WovenClass wovenClass) {
    BundleWiring bw = wovenClass.getBundleWiring();
    
    if (bw != null) {
        Bundle b = bw.getBundle();
        
        if(b.getBundleId() == 0 || 
            b.getSymbolicName().startsWith("org.apache.aries.proxy") ||
            b.getSymbolicName().startsWith("org.apache.aries.util")) {
          return;
        }
    }

    if (!isEnabled(wovenClass.getClassName()) || isDisabled(wovenClass.getClassName())) {
        return;
    }

    if (shouldWeave(wovenClass)) {
      byte[] bytes = null;
      
      try {
        bytes = WovenProxyGenerator.getWovenProxy(wovenClass.getBytes(),
                wovenClass.getBundleWiring().getClassLoader());
        
      } catch (Exception e) {
        if(e instanceof RuntimeException && 
            e.getCause() instanceof UnableToProxyException){
          //This is a weaving failure that should be logged, but the class
          //can still be loaded
          LOGGER.trace(String.format("The class %s cannot be woven, it may not be possible for the runtime to proxy this class.",
                                     wovenClass.getClassName()), e);
        } else {
          throw weavingException(wovenClass, e);
        }
      }
      
      if(bytes != null && bytes.length != 0) {
        wovenClass.setBytes(bytes);
        List<String> imports = wovenClass.getDynamicImports();
        imports.add(IMPORT_A);
        imports.add(IMPORT_B);
      }
    }
  }


    private List<Pattern> parseMatchers(String matchers, String def) {
        String[] strings = (matchers != null ? matchers : def).split(",");
        List<Pattern> patterns = new ArrayList<Pattern>();
        for (String str : strings) {
            str = str.trim();
            if (str.length() != 0) {
                str = str.replaceAll("\\.", "\\\\.");
                str = str.replaceAll("\\*", ".*");
                Pattern p = Pattern.compile(str);
                patterns.add(p);
            }
        }
        return patterns;
    }

    boolean isEnabled(String className) {
        return matches(enabled, className);
    }

    boolean isDisabled(String className) {
        return matches(disabled, className);
    }

    private boolean matches(List<Pattern> patterns, String className) {
        for (Pattern p : patterns) {
            if (p.matcher(className).matches()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isWoven(Class<?> clazz)
    {
      return WovenProxy.class.isAssignableFrom(clazz);
    }

    public boolean isSuperClassWoven(WovenClass wovenClass)
    {
      ClassReader cReader = new ClassReader(wovenClass.getBytes());
      try {
          Class<?> superClass = Class.forName(cReader.getSuperName().replace('/', '.'), false,
                  wovenClass.getBundleWiring().getClassLoader());
          return WovenProxy.class.isAssignableFrom(superClass);
      } catch (ClassNotFoundException e) {
          throw weavingException(wovenClass, e);
      }
    }
    
  private boolean shouldWeave(WovenClass wovenClass)
  {
    // assume we weave
    boolean result = true;
    Object[] cs = controllers.getServices();
    
    // if we have at least 1 weaving controller
    if (cs != null && cs.length > 0) {
      // first of all set to false.
      result = false;
      for (Object obj : cs) {
        ProxyWeavingController c = (ProxyWeavingController) obj;
        if (c.shouldWeave(wovenClass, this)) {
          // exit as soon as we get told to weave, otherwise keep going.
          return true;
        }
      }
    } 
    
    return result;
  }

  private WeavingException weavingException(WovenClass wovenClass, Exception e) {
    String msg = format("There was a serious error trying to weave the class %s. See the associated exception for more information.",
                            wovenClass.getClassName());
    // This is a failure that should stop the class loading!
    LOGGER.error(msg, e);
    return new WeavingException(msg, e);
  }
}