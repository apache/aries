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
package org.apache.aries.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

/**
 * A fake class for testing. We currently need this because the real AriesFrameworkUtil includes multiple
 * layers of static initializers that blow up when not running in a proper OSGi framework.
 */
public final class AriesFrameworkUtil 
{
  
  /**
   * Safely unregister the supplied ServiceRegistration, for when you don't
   * care about the potential IllegalStateException and don't want
   * it to run wild through your code
   * 
   * @param reg The {@link ServiceRegistration}, may be null
   */
  public static void safeUnregisterService(ServiceRegistration reg) 
  {
    if(reg != null) {
      try {
        reg.unregister();
      } catch (IllegalStateException e) {
        //This can be safely ignored
      }
    }
  }
  
  public static ClassLoader getClassLoader(Bundle b) {
      // dummy implementation
      return null;
  }
  
  public static ClassLoader getClassLoaderForced(Bundle b)
  {
      // dummy implementation
      return null;
  }
}
