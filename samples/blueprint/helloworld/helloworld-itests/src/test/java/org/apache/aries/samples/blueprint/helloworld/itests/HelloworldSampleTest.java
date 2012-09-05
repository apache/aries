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
package org.apache.aries.samples.blueprint.helloworld.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import org.osgi.service.blueprint.container.BlueprintContainer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class HelloworldSampleTest extends AbstractIntegrationTest {

    @Test
    public void testBundlesStart() throws Exception {

        /* Check that the HelloWorld Sample bundles are present an started */
        Bundle bapi = getInstalledBundle("org.apache.aries.samples.blueprint.helloworld.api");
        assertNotNull(bapi);
        
        failInBundleNotActiveInFiveSeconds(bapi);
        assertEquals(Bundle.ACTIVE, bapi.getState());

        Bundle bcli = getInstalledBundle("org.apache.aries.samples.blueprint.helloworld.client");
        assertNotNull(bcli);
        failInBundleNotActiveInFiveSeconds(bcli);

        Bundle bser = getInstalledBundle("org.apache.aries.samples.blueprint.helloworld.server");
        assertNotNull(bser);
        failInBundleNotActiveInFiveSeconds(bser);
    }
    
    @Test
    public void testClientBlueprintContainerOnlyStartsWhenServiceStarted() throws Exception
    {
       // Stop everything before we start
       Bundle bcli = getInstalledBundle("org.apache.aries.samples.blueprint.helloworld.client");
       assertNotNull(bcli);
       bcli.stop();

       Bundle bser = getInstalledBundle("org.apache.aries.samples.blueprint.helloworld.server");
       assertNotNull(bser);
       bser.stop();

       // Wait for everything to shut down 
       Thread.sleep(1000);
       
       // When everything is stopped, there should be no blueprint container for either the client or the server 
       
       assertClientBlueprintContainerNull();
       assertServerBlueprintContainerNull();

       // If we start the client first, it shouldn't have a blueprint container
       bcli.start();

       // Wait for everything to get started 
       Thread.sleep(1000);
       assertClientBlueprintContainerNull();
       
       // Then when we start the server both it and the client should have blueprint containers
       bser.start();
       // Wait for everything to get started 
       Thread.sleep(1000);
       assertClientBlueprintContainerNotNull();
       assertServerBlueprintContainerNotNull();

    }
    
    private BlueprintContainer getBlueprintContainer(String bundleName)
    {       
       BlueprintContainer container = null;
       try {
       container = getOsgiService(BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=" + bundleName + ")", 500);
       } catch (RuntimeException e)
       {
          // Just return null if we couldn't get the container
       }
       return container;
    }
    
    private BlueprintContainer getClientBlueprintContainer()
    {
       return getBlueprintContainer("org.apache.aries.samples.blueprint.helloworld.client");
    }
  
    private BlueprintContainer getServerBlueprintContainer()
    {
       return getBlueprintContainer("org.apache.aries.samples.blueprint.helloworld.server");
    }
    
    private void assertClientBlueprintContainerNotNull()
    {
       assertNotNull("There was no blueprint container for the client bundle.", getClientBlueprintContainer());
    }

    private void assertClientBlueprintContainerNull()
    {
       assertNull("There was a blueprint container for the client bundle when we didn't expect one.", getClientBlueprintContainer());
    }

    private void assertServerBlueprintContainerNotNull()
    {
       assertNotNull("There was no blueprint container for the server bundle.", getServerBlueprintContainer());
    }

    private void assertServerBlueprintContainerNull()
    {
       assertNull("There was a blueprint container for the server bundle when we didn't expect one.", getServerBlueprintContainer());
    }


    private void failInBundleNotActiveInFiveSeconds(Bundle bapi)
    {
        for (int i = 0; i < 5 && Bundle.ACTIVE != bapi.getState(); i++) {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
        }
        
        assertEquals("The bundle " + bapi.getSymbolicName() + " " + bapi.getVersion() + " is not active", Bundle.ACTIVE, bapi.getState());
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        Option[] options = options(
                bootDelegationPackages("javax.transaction",
                        "javax.transaction.*"),
                vmOption("-Dorg.osgi.framework.system.packages=javax.accessibility,javax.activation,javax.activity,javax.annotation,javax.annotation.processing,javax.crypto,javax.crypto.interfaces,javax.crypto.spec,javax.imageio,javax.imageio.event,javax.imageio.metadata,javax.imageio.plugins.bmp,javax.imageio.plugins.jpeg,javax.imageio.spi,javax.imageio.stream,javax.jws,javax.jws.soap,javax.lang.model,javax.lang.model.element,javax.lang.model.type,javax.lang.model.util,javax.management,javax.management.loading,javax.management.modelmbean,javax.management.monitor,javax.management.openmbean,javax.management.relation,javax.management.remote,javax.management.remote.rmi,javax.management.timer,javax.naming,javax.naming.directory,javax.naming.event,javax.naming.ldap,javax.naming.spi,javax.net,javax.net.ssl,javax.print,javax.print.attribute,javax.print.attribute.standard,javax.print.event,javax.rmi,javax.rmi.CORBA,javax.rmi.ssl,javax.script,javax.security.auth,javax.security.auth.callback,javax.security.auth.kerberos,javax.security.auth.login,javax.security.auth.spi,javax.security.auth.x500,javax.security.cert,javax.security.sasl,javax.sound.midi,javax.sound.midi.spi,javax.sound.sampled,javax.sound.sampled.spi,javax.sql,javax.sql.rowset,javax.sql.rowset.serial,javax.sql.rowset.spi,javax.swing,javax.swing.border,javax.swing.colorchooser,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.plaf.multi,javax.swing.plaf.synth,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.text.html.parser,javax.swing.text.rtf,javax.swing.tree,javax.swing.undo,javax.tools,javax.xml,javax.xml.bind,javax.xml.bind.annotation,javax.xml.bind.annotation.adapters,javax.xml.bind.attachment,javax.xml.bind.helpers,javax.xml.bind.util,javax.xml.crypto,javax.xml.crypto.dom,javax.xml.crypto.dsig,javax.xml.crypto.dsig.dom,javax.xml.crypto.dsig.keyinfo,javax.xml.crypto.dsig.spec,javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,javax.xml.soap,javax.xml.stream,javax.xml.stream.events,javax.xml.stream.util,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stax,javax.xml.transform.stream,javax.xml.validation,javax.xml.ws,javax.xml.ws.handler,javax.xml.ws.handler.soap,javax.xml.ws.http,javax.xml.ws.soap,javax.xml.ws.spi,javax.xml.xpath,org.ietf.jgss,org.omg.CORBA,org.omg.CORBA.DynAnyPackage,org.omg.CORBA.ORBPackage,org.omg.CORBA.TypeCodePackage,org.omg.CORBA.portable,org.omg.CORBA_2_3,org.omg.CORBA_2_3.portable,org.omg.CosNaming,org.omg.CosNaming.NamingContextExtPackage,org.omg.CosNaming.NamingContextPackage,org.omg.Dynamic,org.omg.DynamicAny,org.omg.DynamicAny.DynAnyFactoryPackage,org.omg.DynamicAny.DynAnyPackage,org.omg.IOP,org.omg.IOP.CodecFactoryPackage,org.omg.IOP.CodecPackage,org.omg.Messaging,org.omg.PortableInterceptor,org.omg.PortableInterceptor.ORBInitInfoPackage,org.omg.PortableServer,org.omg.PortableServer.CurrentPackage,org.omg.PortableServer.POAManagerPackage,org.omg.PortableServer.POAPackage,org.omg.PortableServer.ServantLocatorPackage,org.omg.PortableServer.portable,org.omg.SendingContext,org.omg.stub.java.rmi,org.w3c.dom,org.w3c.dom.bootstrap,org.w3c.dom.css,org.w3c.dom.events,org.w3c.dom.html,org.w3c.dom.ls,org.w3c.dom.ranges,org.w3c.dom.stylesheets,org.w3c.dom.traversal,org.w3c.dom.views,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers,javax.transaction;partial=true;mandatory:=partial,javax.transaction.xa;partial=true;mandatory:=partial"),
                // Log
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
                // Felix mvn url handler - do we need this?
                mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),

                // this is how you set the default log level when using pax
                // logging (logProfile)
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
                        .value("DEBUG"),

                // Bundles
                mavenBundle("org.eclipse.equinox", "cm"),
                mavenBundle("org.eclipse.osgi", "services"),

                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint" ),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
                mavenBundle("org.apache.aries", "org.apache.aries.util" ),
                mavenBundle("org.ow2.asm", "asm-all" ),
                mavenBundle("org.apache.aries.samples.blueprint.helloworld", "org.apache.aries.samples.blueprint.helloworld.api"),
                mavenBundle("org.apache.aries.samples.blueprint.helloworld", "org.apache.aries.samples.blueprint.helloworld.server"),
                mavenBundle("org.apache.aries.samples.blueprint.helloworld", "org.apache.aries.samples.blueprint.helloworld.client"),
                /* For debugging, uncomment the next two lines  */
                /*vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777"),
                waitForFrameworkStartup(),  
*/
                /* For debugging, add these imports:
                import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
                import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
                */
                equinox().version("3.5.0")
        );
        options = updateOptions(options);
        return options;
    }

}
