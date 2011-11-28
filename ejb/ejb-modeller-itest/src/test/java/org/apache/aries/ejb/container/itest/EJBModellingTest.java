/*  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.ejb.container.itest;

import static org.apache.aries.itest.ExtraOptions.mavenBundle;
import static org.apache.aries.itest.ExtraOptions.paxLogging;
import static org.apache.aries.itest.ExtraOptions.testOptions;
import static org.apache.aries.itest.ExtraOptions.transactionBootDelegation;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.ServiceModeller;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class EJBModellingTest extends AbstractEJBModellerTest {

  @Before
  public void setup() {
    mrm = context().getService(ModelledResourceManager.class);
    mm = context().getService(ModellingManager.class);
    context().getService(ServiceModeller.class);
  }

  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
    return testOptions(
        paxLogging("DEBUG"),
        transactionBootDelegation(),
        vmOption("-Dorg.osgi.framework.system.packages.extra=sun.misc,javax.xml.namespace;version=1.1"),
        // Bundles
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
        mavenBundle("asm", "asm-all"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
        mavenBundle("org.apache.aries.ejb", "org.apache.aries.ejb.modeller"),
        mavenBundle("org.apache.openejb", "openejb-core"),
        mavenBundle("org.apache.openejb", "openejb-api"),
        mavenBundle("org.apache.openejb", "openejb-javaagent"),
        mavenBundle("org.apache.openejb", "openejb-jee"),
        mavenBundle("org.apache.openejb", "openejb-loader"),
        mavenBundle("org.apache.openwebbeans", "openwebbeans-impl"),
        mavenBundle("org.apache.openwebbeans", "openwebbeans-spi"),
        mavenBundle("org.apache.openwebbeans", "openwebbeans-ee"),
        mavenBundle("org.apache.openwebbeans", "openwebbeans-ejb"),
        mavenBundle("org.apache.openwebbeans", "openwebbeans-web"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javassist"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.wsdl4j-1.6.1"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxb-impl"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-annotation_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-ejb_3.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_1.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jaxrpc_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-atinject_1.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jsp_2.2_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-saaj_1.3_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-activation_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-j2ee-management_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-j2ee-connector_1.6_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jacc_1.4_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-validation_1.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jaxrs_1.1_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-ws-metadata_2.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jaspic_1.0_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jaxb_2.2_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-stax-api_1.2_spec"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jaxws_2.2_spec"),
        mavenBundle("commons-cli", "commons-cli"),
        mavenBundle("commons-lang", "commons-lang"),
        mavenBundle("commons-beanutils", "commons-beanutils"),
        mavenBundle("commons-collections", "commons-collections"),
        mavenBundle("org.apache.geronimo.components", "geronimo-connector"),
        mavenBundle("org.apache.geronimo.components", "geronimo-transaction"),
        mavenBundle("org.apache.geronimo.bundles", "scannotation"),
        mavenBundle("org.apache.xbean", "xbean-asm-shaded"),
        mavenBundle("org.apache.xbean", "xbean-finder-shaded"),
        mavenBundle("org.apache.xbean", "xbean-naming"),
        mavenBundle("org.apache.xbean", "xbean-reflect"),
        mavenBundle("org.hsqldb", "hsqldb"),
//        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"),
//        waitForFrameworkStartup(),
        

        equinox().version("3.5.0"));
  }

}
