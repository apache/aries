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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.apache.aries.itest.ExtraOptions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.proxy.FinalModifierException;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.container.def.options.CleanCachesOption;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

/**
 * This test runs the weaving proxy tests for the proxy uber bundle. 
 * It needs to be a separate class from the individual-bundle test, since equinox configuration
 * doesn't seem to be cleaned properly within the same test class, so we always run with the 
 * uber-bundle, which means we test nothing.
 *
 */
@RunWith(JUnit4TestRunner.class)
public class WeavingProxyUberBundleTest extends AbstractWeavingProxyTest
{
  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration37UberBundle() {
      return testOptions(
          generalOptions(), 
          proxyUberBundle(),
          equinox37()
      );
  }

}