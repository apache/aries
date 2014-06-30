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
package org.apache.aries.samples.blog.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.inject.Inject;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.samples.blog.api.BloggingService;
import org.apache.aries.samples.blog.api.persistence.BlogPersistenceService;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class AbstractBlogIntegrationTest extends org.apache.aries.itest.AbstractIntegrationTest {

	private static final int CONNECTION_TIMEOUT = 30000;
	public static final long DEFAULT_TIMEOUT = 60000;

	@Inject
	AriesApplicationManager manager;

	protected AriesApplicationContext installEba(MavenArtifactUrlReference eba) throws Exception {
		AriesApplication app = manager.createApplication(new URL(eba.getURL()));
		AriesApplicationContext ctx = manager.install(app);
		ctx.start();
		return ctx;
	}

	protected Bundle assertBundleStarted(String symName) {
		Bundle bundle = context().getBundleByName(symName);
		assertNotNull("Bundle " + symName + " not found", bundle);
		assertEquals(Bundle.ACTIVE, bundle.getState());
		return bundle;
	}

	protected void assertActive(Bundle bundle) {
		assertTrue("Bundle " + bundle.getSymbolicName() + " should be ACTIVE but is in state " + bundle.getState(), bundle.getState() == Bundle.ACTIVE);
	}

	protected void assertResolved(Bundle bundle) {
		assertTrue("Bundle " + bundle.getSymbolicName() + " should be ACTIVE but is in state " + bundle.getState(), bundle.getState() == Bundle.RESOLVED);
	}

	@SuppressWarnings("rawtypes")
	protected void listBundleServices(Bundle b) {
		ServiceReference []srb = b.getRegisteredServices();
		for(ServiceReference sr:srb){
			System.out.println(b.getSymbolicName() + " SERVICE: "+sr);
		}	
	}

	@SuppressWarnings("rawtypes")
	protected Boolean isServiceRegistered(Bundle b) {
		ServiceReference []srb = b.getRegisteredServices();
		if(srb == null) {
			return false;
		}
		return true;
	}

	protected void checkBlogWebAccess() throws IOException, InterruptedException {
		Thread.sleep(1000);
		HttpURLConnection conn = makeConnection("http://localhost:8080/blog/ViewBlog");
		String response = getHTTPResponse(conn);

		/* Uncomment for additional debug */
		/*
		System.out.println("ZZZZZ " + response);
		System.out.println("ZZZZZ " + conn.getResponseCode());
		System.out.println("ZZZZZ " + HttpURLConnection.HTTP_OK);
		 */

		assertEquals(HttpURLConnection.HTTP_OK,
				conn.getResponseCode());

		assertTrue("The response did not contain the expected content", response.contains("Blog home"));
	}

	public static String getHTTPResponse(HttpURLConnection conn) throws IOException
	{
		StringBuilder response = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),
				"ISO-8859-1"));
		try {
			for (String s = reader.readLine(); s != null; s = reader.readLine()) {
				response.append(s).append("\r\n");
			}
		} finally {
			reader.close();
		}

		return response.toString();
	}

	public static HttpURLConnection makeConnection(String contextPath) throws IOException
	{
		URL url = new URL(contextPath);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setConnectTimeout(CONNECTION_TIMEOUT);
		conn.connect();

		return conn;
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
		return options(
				baseOptions(),
				frameworkProperty("org.osgi.framework.system.packages")
				.value("javax.accessibility,javax.activation,javax.activity,javax.annotation,javax.annotation.processing,javax.crypto,javax.crypto.interfaces,javax.crypto.spec,javax.imageio,javax.imageio.event,javax.imageio.metadata,javax.imageio.plugins.bmp,javax.imageio.plugins.jpeg,javax.imageio.spi,javax.imageio.stream,javax.jws,javax.jws.soap,javax.lang.model,javax.lang.model.element,javax.lang.model.type,javax.lang.model.util,javax.management,javax.management.loading,javax.management.modelmbean,javax.management.monitor,javax.management.openmbean,javax.management.relation,javax.management.remote,javax.management.remote.rmi,javax.management.timer,javax.naming,javax.naming.directory,javax.naming.event,javax.naming.ldap,javax.naming.spi,javax.net,javax.net.ssl,javax.print,javax.print.attribute,javax.print.attribute.standard,javax.print.event,javax.rmi,javax.rmi.CORBA,javax.rmi.ssl,javax.script,javax.security.auth,javax.security.auth.callback,javax.security.auth.kerberos,javax.security.auth.login,javax.security.auth.spi,javax.security.auth.x500,javax.security.cert,javax.security.sasl,javax.sound.midi,javax.sound.midi.spi,javax.sound.sampled,javax.sound.sampled.spi,javax.sql,javax.sql.rowset,javax.sql.rowset.serial,javax.sql.rowset.spi,javax.swing,javax.swing.border,javax.swing.colorchooser,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.plaf.multi,javax.swing.plaf.synth,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.text.html.parser,javax.swing.text.rtf,javax.swing.tree,javax.swing.undo,javax.tools,javax.xml,javax.xml.bind,javax.xml.bind.annotation,javax.xml.bind.annotation.adapters,javax.xml.bind.attachment,javax.xml.bind.helpers,javax.xml.bind.util,javax.xml.crypto,javax.xml.crypto.dom,javax.xml.crypto.dsig,javax.xml.crypto.dsig.dom,javax.xml.crypto.dsig.keyinfo,javax.xml.crypto.dsig.spec,javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,javax.xml.soap,javax.xml.stream,javax.xml.stream.events,javax.xml.stream.util,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stax,javax.xml.transform.stream,javax.xml.validation,javax.xml.ws,javax.xml.ws.handler,javax.xml.ws.handler.soap,javax.xml.ws.http,javax.xml.ws.soap,javax.xml.ws.spi,javax.xml.xpath,org.ietf.jgss,org.omg.CORBA,org.omg.CORBA.DynAnyPackage,org.omg.CORBA.ORBPackage,org.omg.CORBA.TypeCodePackage,org.omg.CORBA.portable,org.omg.CORBA_2_3,org.omg.CORBA_2_3.portable,org.omg.CosNaming,org.omg.CosNaming.NamingContextExtPackage,org.omg.CosNaming.NamingContextPackage,org.omg.Dynamic,org.omg.DynamicAny,org.omg.DynamicAny.DynAnyFactoryPackage,org.omg.DynamicAny.DynAnyPackage,org.omg.IOP,org.omg.IOP.CodecFactoryPackage,org.omg.IOP.CodecPackage,org.omg.Messaging,org.omg.PortableInterceptor,org.omg.PortableInterceptor.ORBInitInfoPackage,org.omg.PortableServer,org.omg.PortableServer.CurrentPackage,org.omg.PortableServer.POAManagerPackage,org.omg.PortableServer.POAPackage,org.omg.PortableServer.ServantLocatorPackage,org.omg.PortableServer.portable,org.omg.SendingContext,org.omg.stub.java.rmi,org.w3c.dom,org.w3c.dom.bootstrap,org.w3c.dom.css,org.w3c.dom.events,org.w3c.dom.html,org.w3c.dom.ls,org.w3c.dom.ranges,org.w3c.dom.stylesheets,org.w3c.dom.traversal,org.w3c.dom.views,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers"),
				// Log
				//mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
				//mavenBundle("org.ops4j.pax.logging", "pax-logging-service").versionAsInProject(),
				// Felix mvn url handler - do we need this?
				//mavenBundle("org.ops4j.pax.url", "pax-url-aether").versionAsInProject(),

				mavenBundle("org.eclipse.equinox", "cm").versionAsInProject(),
				mavenBundle("org.eclipse.osgi", "services").versionAsInProject(),

				mavenBundle("org.apache.xbean", "xbean-asm4-shaded").versionAsInProject(),
				mavenBundle("org.apache.xbean", "xbean-finder-shaded").versionAsInProject(),
				mavenBundle("org.ops4j.pax.web", "pax-web-jetty-bundle").versionAsInProject(),
				mavenBundle("org.ops4j.pax.web", "pax-web-extender-war").versionAsInProject(),

				//mavenBundle("org.ops4j.pax.web", "pax-web-jsp").versionAsInProject(),
				mavenBundle("org.apache.derby", "derby").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec").versionAsInProject(),

				mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-j2ee-connector_1.5_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_2.5_spec").versionAsInProject(),
				mavenBundle("org.apache.geronimo.components", "geronimo-transaction").versionAsInProject(),
				mavenBundle("org.apache.openjpa", "openjpa").versionAsInProject(),
				mavenBundle("commons-lang", "commons-lang").versionAsInProject(),
				mavenBundle("commons-collections", "commons-collections").versionAsInProject(),
				mavenBundle("commons-pool", "commons-pool").versionAsInProject(),
				mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp").versionAsInProject(),
				mavenBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.api").versionAsInProject(),
	            mavenBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.manager").versionAsInProject(),
				mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint" ).versionAsInProject(),
				mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
				mavenBundle("org.apache.aries", "org.apache.aries.util" ).versionAsInProject(),
				mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi" ).versionAsInProject(),
				mavenBundle("org.apache.felix", "org.apache.felix.fileinstall" ).versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.install" ).versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.api" ).versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.management" ).versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime" ).versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils" ).versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform").versionAsInProject(),
				mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management").versionAsInProject(),
				mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api" ).versionAsInProject(),
				mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container" ).versionAsInProject(),
				mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.blueprint.aries" ).versionAsInProject(),
				mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.context" ).versionAsInProject(),
				mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager" ).versionAsInProject(),
				mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.blueprint" ).versionAsInProject(),
				mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.wrappers" ).versionAsInProject(),
				mavenBundle("org.ow2.asm", "asm-all" ).versionAsInProject(),
				mavenBundle("org.apache.aries.samples.blog", "org.apache.aries.samples.blog.datasource" ).versionAsInProject()
				///vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777"),
				);
	}

	protected void assertBlogServicesStarted() {
		context().getService(BloggingService.class);
		context().getService(BlogPersistenceService.class);
		context().getService(XADataSource.class);
		context().getService(TransactionManager.class);
	}

}
