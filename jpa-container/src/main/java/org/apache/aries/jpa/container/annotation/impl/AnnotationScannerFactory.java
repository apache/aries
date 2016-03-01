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
package org.apache.aries.jpa.container.annotation.impl;

/**
 * Creates an {@link AnnotationScanner} on startup unless BundleWiring support
 * is unavailable
 */
public class AnnotationScannerFactory {

private static final AnnotationScanner _instance;
  
  static {
    AnnotationScanner tr = null;
    try {
      Class.forName("org.osgi.framework.wiring.BundleWiring");
      tr = new JPAAnnotationScanner();
    } catch (ClassNotFoundException cnfe) {
      //TODO log this
    } catch (Exception e) {
      throw new RuntimeException(e);
    } 
    _instance = tr;
  }
  /**
   * Get the current {@link AnnotationScanner}, or null if none is available
   * @return
   */
  public static AnnotationScanner getAnnotationScanner() {
    return _instance;
  }
}
