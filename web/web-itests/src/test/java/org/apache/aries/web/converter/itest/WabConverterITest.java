/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
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
package org.apache.aries.web.converter.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

import javax.inject.Inject;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class WabConverterITest extends AbstractIntegrationTest {
	@Inject
	protected BundleContext bundleContext;

	private void createTestWar(File warFile) throws IOException {
		ZipFixture testWar = ArchiveFixture.newJar().binary(
				"WEB-INF/classes/org/apache/aries/web/test/TestClass.class",
				getClass().getClassLoader().getResourceAsStream(
						"org/apache/aries/web/test/TestClass.class"));

		FileOutputStream fout = new FileOutputStream(warFile);
		testWar.writeOut(fout);
		fout.close();
	}

	@Test
	public void getStarted() throws Exception {
		File testWar = File.createTempFile("test", ".war");
		createTestWar(testWar);
		String baseUrl = "webbundle:"
				+ testWar.toURI().toURL().toExternalForm();
		assertTrue("Time out waiting for webbundle URL handler",
				waitForURLHandler(baseUrl));

		Bundle converted = bundleContext.installBundle(baseUrl
				+ "?Bundle-SymbolicName=test.war.bundle&Web-ContextPath=foo");

		assertNotNull(converted);
		Dictionary<String, String> man = converted.getHeaders();

		assertEquals("test.war.bundle", man.get(Constants.BUNDLE_SYMBOLICNAME));
		assertEquals("/foo", man.get("Web-ContextPath"));
		assertTrue(man.get(Constants.IMPORT_PACKAGE).contains("javax.naming"));
		new File("test.war").delete();
	}

	private boolean waitForURLHandler(String url) {
		int maxRepetition = 100;
		for (int i = 0; i < maxRepetition; i++) {
			try {
				new URL(url);
				return true;
			} catch (MalformedURLException e) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException ee) {
					return false;
				}
			}
		}
		return false;
	}

	public Option baseOptions() {
		String localRepo = getLocalRepo();
		return composite(
				junitBundles(),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
				when(localRepo != null).useOptions(
						vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo)));
	}

	@Configuration
	public Option[] configuration() {
		return options(
				// bootDelegation(),
				baseOptions(),
				mavenBundle("org.osgi", "org.osgi.compendium"),
				mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),

				// Bundles
				mavenBundle("org.apache.aries.web", "org.apache.aries.web.urlhandler"),
				mavenBundle("org.apache.aries", "org.apache.aries.util"),
				mavenBundle("org.ow2.asm", "asm-all"),
				mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
				mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
				mavenBundle("org.apache.aries.testsupport",	"org.apache.aries.testsupport.unit"));
	}

	private MavenArtifactProvisionOption mavenBundle(String groupId,
			String artifactId) {
		return CoreOptions.mavenBundle().groupId(groupId)
				.artifactId(artifactId).versionAsInProject();
	}

}
