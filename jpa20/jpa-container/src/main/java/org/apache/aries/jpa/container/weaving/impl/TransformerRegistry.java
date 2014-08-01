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

import javax.persistence.spi.ClassTransformer;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * {@link ClassTransformer} instances should be registered with the
 * instance of this interface returned by {@link TransformerRegistryFactory#getTransformerRegistry()}
 */
public interface TransformerRegistry {

  /**
   * Register a new transformer with the WeavingHook
   * 
   * @param pBundle  The persistence bundle to weave
   * @param transformer  The transformer to weave with
   * @param provider The provider to provide packages from
   */
  public void addTransformer(Bundle pBundle, ClassTransformer transformer, ServiceReference<?> provider);
  
  
  /**
   * Remove a given transformer from this weaving hook. This must be 
   * @param pBundle
   * @param transformer
   */
  public void removeTransformer(Bundle pBundle, ClassTransformer transformer);
}
