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
package org.apache.aries.jmx.cm;

import org.apache.aries.itest.ExtraOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;

public class ConfigurationAdminMBeanWhiteboardTest extends ConfigurationAdminMBeanTest
{
  @Configuration
  public static Option[] configuration() {
    Option[] options = ConfigurationAdminMBeanTest.configuration();
    
    for (int i = 0; i < options.length; i++)
    {
      if (options[i] instanceof MavenArtifactProvisionOption) {
        MavenArtifactProvisionOption po = (MavenArtifactProvisionOption) options[i];
        String url = po.getURL();
        if (url.contains("mvn:org.apache.aries.jmx/org.apache.aries.jmx/")) {
          options[i] = ExtraOptions.mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.core.whiteboard");
        }
      }
    }
    
    return options;
  }
}