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
package org.apache.aries.application.management.spi.resolve;

import java.util.Collection;

import org.apache.aries.application.modelling.ModelledResource;

/**
 * This interface allows a pre resolve hook to add customizats
 * into the OBR resolve operation.
 */
public interface PreResolveHook 
{
  /**
   * Depending on the environment it may be necessary to add
   * resources to the resolve operation which you do not wish
   * to provision. These may be resources that already exist 
   * and are available, or are sourced in a different way. Any
   * resources returned by this method are resolved against, but
   * not placed in the deployment.mf. This may result in problems
   * if a fake resource is provided, but the capabilities are not
   * provided at runtime.
   * 
   * @param resources A mutable collection of ModelledResources that can have
   *                  more elements added or removed.
   */
  public void collectFakeResources(Collection<ModelledResource> resources);
}