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

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.ServiceModeller;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleException;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class EJBModellingTest extends AbstractEJBModellerTest {

	@Before
	public void setup() throws BundleException {
//IC see: https://issues.apache.org/jira/browse/ARIES-1213
		resolveBundles();
		mrm = context().getService(ModelledResourceManager.class);
		mm = context().getService(ModellingManager.class);
		context().getService(ServiceModeller.class);
	}

	protected Option baseOptions() {
		String localRepo = System.getProperty("maven.repo.local");

		if (localRepo == null) {
			localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
		}
		return composite(
				junitBundles(),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.7.2"),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.7.2"),
				mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),
				// this is how you set the default log level when using pax
				// logging (logProfile)
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
				when(localRepo != null).useOptions(vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo))
				);
	}

	@Configuration
	public Option[] configuration() {
		return CoreOptions.options(
				baseOptions(),
				frameworkProperty("org.osgi.framework.system.packages.extra").value("sun.misc,javax.xml.namespace;version=1.1"),
				frameworkProperty("org.osgi.framework.system.packages")
				.value("javax.accessibility,javax.activation,javax.activity,javax.annotation,javax.annotation.processing,javax.crypto,javax.crypto.interfaces,javax.crypto.spec,javax.imageio,javax.imageio.event,javax.imageio.metadata,javax.imageio.plugins.bmp,javax.imageio.plugins.jpeg,javax.imageio.spi,javax.imageio.stream,javax.jws,javax.jws.soap,javax.lang.model,javax.lang.model.element,javax.lang.model.type,javax.lang.model.util,javax.management,javax.management.loading,javax.management.modelmbean,javax.management.monitor,javax.management.openmbean,javax.management.relation,javax.management.remote,javax.management.remote.rmi,javax.management.timer,javax.naming,javax.naming.directory,javax.naming.event,javax.naming.ldap,javax.naming.spi,javax.net,javax.net.ssl,javax.print,javax.print.attribute,javax.print.attribute.standard,javax.print.event,javax.rmi,javax.rmi.CORBA,javax.rmi.ssl,javax.script,javax.security.auth,javax.security.auth.callback,javax.security.auth.kerberos,javax.security.auth.login,javax.security.auth.spi,javax.security.auth.x500,javax.security.cert,javax.security.sasl,javax.sound.midi,javax.sound.midi.spi,javax.sound.sampled,javax.sound.sampled.spi,javax.sql,javax.sql.rowset,javax.sql.rowset.serial,javax.sql.rowset.spi,javax.swing,javax.swing.border,javax.swing.colorchooser,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.plaf.multi,javax.swing.plaf.synth,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.text.html.parser,javax.swing.text.rtf,javax.swing.tree,javax.swing.undo,javax.tools,javax.xml,javax.xml.bind,javax.xml.bind.annotation,javax.xml.bind.annotation.adapters,javax.xml.bind.attachment,javax.xml.bind.helpers,javax.xml.bind.util,javax.xml.crypto,javax.xml.crypto.dom,javax.xml.crypto.dsig,javax.xml.crypto.dsig.dom,javax.xml.crypto.dsig.keyinfo,javax.xml.crypto.dsig.spec,javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,javax.xml.soap,javax.xml.stream,javax.xml.stream.events,javax.xml.stream.util,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stax,javax.xml.transform.stream,javax.xml.validation,javax.xml.ws,javax.xml.ws.handler,javax.xml.ws.handler.soap,javax.xml.ws.http,javax.xml.ws.soap,javax.xml.ws.spi,javax.xml.xpath,org.ietf.jgss,org.omg.CORBA,org.omg.CORBA.DynAnyPackage,org.omg.CORBA.ORBPackage,org.omg.CORBA.TypeCodePackage,org.omg.CORBA.portable,org.omg.CORBA_2_3,org.omg.CORBA_2_3.portable,org.omg.CosNaming,org.omg.CosNaming.NamingContextExtPackage,org.omg.CosNaming.NamingContextPackage,org.omg.Dynamic,org.omg.DynamicAny,org.omg.DynamicAny.DynAnyFactoryPackage,org.omg.DynamicAny.DynAnyPackage,org.omg.IOP,org.omg.IOP.CodecFactoryPackage,org.omg.IOP.CodecPackage,org.omg.Messaging,org.omg.PortableInterceptor,org.omg.PortableInterceptor.ORBInitInfoPackage,org.omg.PortableServer,org.omg.PortableServer.CurrentPackage,org.omg.PortableServer.POAManagerPackage,org.omg.PortableServer.POAPackage,org.omg.PortableServer.ServantLocatorPackage,org.omg.PortableServer.portable,org.omg.SendingContext,org.omg.stub.java.rmi,org.w3c.dom,org.w3c.dom.bootstrap,org.w3c.dom.css,org.w3c.dom.events,org.w3c.dom.html,org.w3c.dom.ls,org.w3c.dom.ranges,org.w3c.dom.stylesheets,org.w3c.dom.traversal,org.w3c.dom.views,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers"),

				// Specs
				mavenBundle("org.osgi", "org.osgi.compendium"),
				mavenBundle("org.apache.geronimo.specs", "geronimo-annotation_1.1_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-ejb_3.1_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_1.0_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jaxrpc_1.1_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jsp_2.2_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-saaj_1.3_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-activation_1.1_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-j2ee-management_1.1_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-j2ee-connector_1.6_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jacc_1.4_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-validation_1.0_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jaxrs_1.1_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-ws-metadata_2.0_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jaspic_1.0_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jaxb_2.2_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-stax-api_1.2_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jaxws_2.2_spec").versionAsInProject(),
				
				mavenBundle("commons-cli", "commons-cli").versionAsInProject(),
				mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
				mavenBundle("commons-lang", "commons-lang").versionAsInProject(),
				mavenBundle("commons-beanutils", "commons-beanutils").versionAsInProject(),
				mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.commons-collections").versionAsInProject(),
				
				mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
				mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
				mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint").versionAsInProject(),
				mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),

				mavenBundle("org.apache.aries.application", "org.apache.aries.application.api").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils").versionAsInProject(),
				mavenBundle("org.apache.aries.ejb", "org.apache.aries.ejb.modeller").versionAsInProject(),

				mavenBundle("org.apache.openejb", "openejb-core").versionAsInProject(),
				mavenBundle("org.apache.openejb", "openejb-api").versionAsInProject(),
				mavenBundle("org.apache.openejb", "openejb-javaagent").versionAsInProject(),
				mavenBundle("org.apache.openejb", "openejb-jee").versionAsInProject(),
				mavenBundle("org.apache.openejb", "openejb-loader").versionAsInProject(),
				
				mavenBundle("org.apache.openwebbeans", "openwebbeans-impl").versionAsInProject(),
				mavenBundle("org.apache.openwebbeans", "openwebbeans-spi").versionAsInProject(),
				mavenBundle("org.apache.openwebbeans", "openwebbeans-ee").versionAsInProject(),
				mavenBundle("org.apache.openwebbeans", "openwebbeans-ejb").versionAsInProject(),
				mavenBundle("org.apache.openwebbeans", "openwebbeans-web").versionAsInProject(),

				mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javassist").versionAsInProject(),
				mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.wsdl4j-1.6.1").versionAsInProject(),
				mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxb-impl").versionAsInProject(),
				
				
				mavenBundle("org.apache.geronimo.components", "geronimo-connector").versionAsInProject(),
				mavenBundle("org.apache.geronimo.components", "geronimo-transaction").versionAsInProject(),
				mavenBundle("org.apache.geronimo.bundles", "scannotation").versionAsInProject(),
				mavenBundle("org.apache.xbean", "xbean-asm-shaded").versionAsInProject(),
				mavenBundle("org.apache.xbean", "xbean-finder-shaded").versionAsInProject(),
				mavenBundle("org.apache.xbean", "xbean-naming").versionAsInProject(),
				mavenBundle("org.apache.xbean", "xbean-reflect").versionAsInProject(),
				mavenBundle("org.hsqldb", "hsqldb").versionAsInProject()
				);
		//        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"),
		//        waitForFrameworkStartup(),
	}

}
