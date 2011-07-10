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
package org.apache.aries.itest;

import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;

public class ExtraOptions {
    public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).versionAsInProject();
    }
    
    /**
     * For use in the actual test code, which runs in OSGi and has no normal access to the configuration options
     * @param groupId
     * @param artifactId
     * @return
     */
    public static MavenArtifactProvisionOption mavenBundleInTest(ClassLoader loader, String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId)
            .version(getArtifactVersion(loader, groupId, artifactId));
    }

    //TODO getArtifactVersion and getFileFromClasspath are borrowed and modified from pax-exam.  They should be moved back ASAP.
    private static String getArtifactVersion( ClassLoader loader, final String groupId, final String artifactId )
    {
        final Properties dependencies = new Properties();
        try
        {
            InputStream in = getFileFromClasspath(loader, "META-INF/maven/dependencies.properties");
            try {
                dependencies.load(in);
            } finally {
                in.close();
            }
            final String version = dependencies.getProperty( groupId + "/" + artifactId + "/version" );
            if( version == null )
            {
                throw new RuntimeException(
                    "Could not resolve version. Do you have a dependency for " + groupId + "/" + artifactId
                    + " in your maven project?"
                );
            }
            return version;
        }
        catch( IOException e )
        {
            // TODO throw a better exception
            throw new RuntimeException(
                "Could not resolve version. Did you configured the plugin in your maven project?"
                + "Or maybe you did not run the maven build and you are using an IDE?"
            );
        }
    }


    private static InputStream getFileFromClasspath(ClassLoader loader,  final String filePath )
        throws FileNotFoundException
    {
        try
        {
            URL fileURL = loader.getResource( filePath );
            if( fileURL == null )
            {
                throw new FileNotFoundException( "File [" + filePath + "] could not be found in classpath" );
            }
            return fileURL.openStream();
        }
        catch (IOException e)
        {
            throw new FileNotFoundException( "File [" + filePath + "] could not be found: " + e.getMessage() );
        }
    }    
    
    /**
     * Add the pax logging provider with the given default logging level
     */
    public static Option[] paxLogging(String defaultLogLevel) {
        return flatOptions(
                bundles("org.ops4j.pax.logging/pax-logging-api",
                        "org.ops4j.pax.logging/pax-logging-service"),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value(defaultLogLevel)
        );
    }
    
    public static Option[] transactionBootDelegation() {
        return options(
                bootDelegationPackages("javax.transaction", "javax.transaction.*"),
                vmOption("-Dorg.osgi.framework.system.packages=javax.accessibility,javax.activation,javax.activity,javax.annotation,javax.annotation.processing,javax.crypto,javax.crypto.interfaces,javax.crypto.spec,javax.imageio,javax.imageio.event,javax.imageio.metadata,javax.imageio.plugins.bmp,javax.imageio.plugins.jpeg,javax.imageio.spi,javax.imageio.stream,javax.jws,javax.jws.soap,javax.lang.model,javax.lang.model.element,javax.lang.model.type,javax.lang.model.util,javax.management,javax.management.loading,javax.management.modelmbean,javax.management.monitor,javax.management.openmbean,javax.management.relation,javax.management.remote,javax.management.remote.rmi,javax.management.timer,javax.naming,javax.naming.directory,javax.naming.event,javax.naming.ldap,javax.naming.spi,javax.net,javax.net.ssl,javax.print,javax.print.attribute,javax.print.attribute.standard,javax.print.event,javax.rmi,javax.rmi.CORBA,javax.rmi.ssl,javax.script,javax.security.auth,javax.security.auth.callback,javax.security.auth.kerberos,javax.security.auth.login,javax.security.auth.spi,javax.security.auth.x500,javax.security.cert,javax.security.sasl,javax.sound.midi,javax.sound.midi.spi,javax.sound.sampled,javax.sound.sampled.spi,javax.sql,javax.sql.rowset,javax.sql.rowset.serial,javax.sql.rowset.spi,javax.swing,javax.swing.border,javax.swing.colorchooser,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.plaf.multi,javax.swing.plaf.synth,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.text.html.parser,javax.swing.text.rtf,javax.swing.tree,javax.swing.undo,javax.tools,javax.xml,javax.xml.bind,javax.xml.bind.annotation,javax.xml.bind.annotation.adapters,javax.xml.bind.attachment,javax.xml.bind.helpers,javax.xml.bind.util,javax.xml.crypto,javax.xml.crypto.dom,javax.xml.crypto.dsig,javax.xml.crypto.dsig.dom,javax.xml.crypto.dsig.keyinfo,javax.xml.crypto.dsig.spec,javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,javax.xml.soap,javax.xml.stream,javax.xml.stream.events,javax.xml.stream.util,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stax,javax.xml.transform.stream,javax.xml.validation,javax.xml.ws,javax.xml.ws.handler,javax.xml.ws.handler.soap,javax.xml.ws.http,javax.xml.ws.soap,javax.xml.ws.spi,javax.xml.xpath,org.ietf.jgss,org.omg.CORBA,org.omg.CORBA.DynAnyPackage,org.omg.CORBA.ORBPackage,org.omg.CORBA.TypeCodePackage,org.omg.CORBA.portable,org.omg.CORBA_2_3,org.omg.CORBA_2_3.portable,org.omg.CosNaming,org.omg.CosNaming.NamingContextExtPackage,org.omg.CosNaming.NamingContextPackage,org.omg.Dynamic,org.omg.DynamicAny,org.omg.DynamicAny.DynAnyFactoryPackage,org.omg.DynamicAny.DynAnyPackage,org.omg.IOP,org.omg.IOP.CodecFactoryPackage,org.omg.IOP.CodecPackage,org.omg.Messaging,org.omg.PortableInterceptor,org.omg.PortableInterceptor.ORBInitInfoPackage,org.omg.PortableServer,org.omg.PortableServer.CurrentPackage,org.omg.PortableServer.POAManagerPackage,org.omg.PortableServer.POAPackage,org.omg.PortableServer.ServantLocatorPackage,org.omg.PortableServer.portable,org.omg.SendingContext,org.omg.stub.java.rmi,org.w3c.dom,org.w3c.dom.bootstrap,org.w3c.dom.css,org.w3c.dom.events,org.w3c.dom.html,org.w3c.dom.ls,org.w3c.dom.ranges,org.w3c.dom.stylesheets,org.w3c.dom.traversal,org.w3c.dom.views,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers,javax.transaction;partial=true;mandatory:=partial,javax.transaction.xa;partial=true;mandatory:=partial")                
        );
    }
    
    /**
     * Convert a list of options or Option[] into a flat list of options
     * @param params
     * @return
     */
    public static Option[] flatOptions(Object ... params) {
        List<Option> result = new ArrayList<Option>();
        
        for (Object p : params) {
            if (p instanceof Option) {
                result.add((Option) p);
            } else if (p.getClass().isArray()) {
                result.addAll(Arrays.asList(flatOptions((Object[]) p)));
            }
        }
        
        return result.toArray(new Option[0]);
    }
    
    /**
     * Top level option combinator that adds platform fixes as needed
     * @param params
     * @return
     */
    public static Option[] testOptions(Object ... params) {
        return combine(flatOptions(params),
                when("IBM Corporation".equals(System.getProperty("java.vendor")))
                    .useOptions(wrappedBundle(mavenBundle("org.ops4j.pax.exam", "pax-exam-junit"))),
                mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit")
        );
    }
    
    /**
     * Specify a collection of bundles
     * @param specs Bundle specs of the format &lt;group&gt;/&lt;artifact&gt;[/&lt;version&gt;]
     * @return
     */
    public static Option[] bundles(String ... specs) {
        List<Option> result = new ArrayList<Option>();
        
        for (String spec : specs) {
            String[] parts = spec.split("/");
            
            if (parts.length == 2) {
                result.add(mavenBundle(parts[0], parts[1]));
            } else if (parts.length == 3) {
                result.add(CoreOptions.mavenBundle(parts[0], parts[1], parts[2]));
            } else {
                throw new IllegalArgumentException(
                        "Ill-formatted bundle spec: '"+spec+"'. Expected format <group>/<artifact>[/<version>].");
            }
        }
        
        return result.toArray(new Option[0]);
    }

}
