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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.weaving.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

/**
 * This class is used to get hold of the active {@link TransformerRegistry}
 * for this bundle. 
 */
public class TransformerRegistryFactory {
  private static final TransformerRegistry _instance;
  private static final int mask = Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING;
  
  static {
    TransformerRegistry tr = null;
    try {
      tr = (TransformerRegistry) Class.forName("org.apache.aries.jpa.container.weaving.impl.JPAWeavingHook").newInstance();
      Bundle b = FrameworkUtil.getBundle(tr.getClass());
      if(b != null && (b.getState() & mask) != 0) {
        // ARIES-1019: Register with the highest possible service ranking to
        // avoid ClassNotFoundException caused by interfaces added by earlier
        // weaving hooks that are not yet visible to the bundle class loader.
        Dictionary<String, Object> props = new Hashtable<String, Object>(1);
        props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        b.getBundleContext().registerService(
            "org.osgi.framework.hooks.weaving.WeavingHook", tr, props);
      }
    } catch (NoClassDefFoundError ncdfe) {
      //TODO log this
    } catch (Exception e) {
      throw new RuntimeException(e);
    } 
    _instance = tr;
  }
  
  /**
   * @return the active {@link TransformerRegistry} or null if no transformation
   * support is available
   */
  public static TransformerRegistry getTransformerRegistry() {
    return _instance;
  }
}
