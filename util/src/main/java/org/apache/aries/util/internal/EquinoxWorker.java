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
package org.apache.aries.util.internal;

import java.lang.reflect.InvocationTargetException;

import org.osgi.framework.Bundle;

public class EquinoxWorker extends DefaultWorker implements FrameworkUtilWorker {

  public ClassLoader getClassLoader(Bundle b) 
  {
    ClassLoader result = null;
    try {
      Object bundleLoaderProxy = invoke(b, "getLoaderProxy");
      if (bundleLoaderProxy != null) {
        Object bundleLoader = invoke(bundleLoaderProxy, "getBasicBundleLoader");
        if (bundleLoader != null) {
          Object bundleClassLoader = invoke(bundleLoader, "createClassLoader");
          if (bundleClassLoader instanceof ClassLoader) {
            result = (ClassLoader)bundleClassLoader;
          }
        }
      }
    } catch (IllegalArgumentException e) {
    } catch (SecurityException e) {
    } catch (IllegalAccessException e) {
    } catch (InvocationTargetException e) {
    } catch (NoSuchMethodException e) {
    }
    
    return result;
  }

  private Object invoke(Object targetObject, String method) throws IllegalAccessException, InvocationTargetException,
      NoSuchMethodException
  {
    return targetObject.getClass().getDeclaredMethod(method).invoke(targetObject);
  }
}
