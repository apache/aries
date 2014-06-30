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
package org.apache.aries.proxy.itests;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

/**
 * This test runs the weaving proxy tests for the proxy bundles (not the uber bundle). 
 * It needs to be a separate class from the uber-bundle test, since equinox configuration
 * doesn't seem to be cleaned properly within the same test class, so we run with the 
 * uber-bundle, which means we test nothing.
 *
 */
public class WeavingProxyBundlesTest extends AbstractWeavingProxyTest
{
  @Configuration
  public Option[] configuration() {
      return proxyBundles();
  }
}