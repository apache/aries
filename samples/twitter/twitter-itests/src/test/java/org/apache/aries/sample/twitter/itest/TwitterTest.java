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
package org.apache.aries.sample.twitter.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

@RunWith(PaxExam.class)
public class TwitterTest extends AbstractIntegrationTest 
{
	public static final String CORE_BUNDLE_BY_VALUE = "core.bundle.by.value";
	public static final String CORE_BUNDLE_BY_REFERENCE = "core.bundle.by.reference";
	public static final String TRANSITIVE_BUNDLE_BY_VALUE = "transitive.bundle.by.value";
	public static final String TRANSITIVE_BUNDLE_BY_REFERENCE = "transitive.bundle.by.reference";
	public static final String USE_BUNDLE_BY_REFERENCE = "use.bundle.by.reference";
	public static final String REPO_BUNDLE = "aries.bundle1";
	public static final String HELLO_WORLD_CLIENT_BUNDLE="hello.world.client.bundle";
	public static final String HELLO_WORLD_SERVICE_BUNDLE1="hello.world.service.bundle1";
	public static final String HELLO_WORLD_SERVICE_BUNDLE2="hello.world.service.bundle2";

	@Inject
	RepositoryAdmin repositoryAdmin;

	@Inject
	AriesApplicationManager manager;

	/**
	 * Test for ARIES-461
	 * Application that bring in dependency bundles from a bundle repository doesn't deploy
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTwitter() throws Exception
	{
		// provision against the local runtime
		System.setProperty(AppConstants.PROVISON_EXCLUDE_LOCAL_REPO_SYSPROP, "false");

		deleteRepos();

		MavenArtifactUrlReference twitterEbaUrl = maven("org.apache.aries.samples.twitter", "org.apache.aries.samples.twitter.eba").versionAsInProject().type("eba");
		MavenArtifactUrlReference twitterCommonLangJar = maven("commons-lang", "commons-lang").versionAsInProject();
		MavenArtifactUrlReference twitterJar = maven("org.apache.aries.samples.twitter", "org.apache.aries.samples.twitter.twitter4j").versionAsInProject();

		// add the repository xml to the repository admin
		String repositoryXML = getRepoContent("/obr/twitter/TwitterRepository.xml");
		// replace the jar file url with the real url related to the environment
		String repo = repositoryXML
				.replaceAll("commons.lang.location", twitterCommonLangJar.getURL())
				.replaceAll("twitter4j.location", twitterJar.getURL());

		URL url = getRepoUrl(repo);
		repositoryAdmin.addRepository(url);

		AriesApplication app = manager.createApplication(new URL(twitterEbaUrl.getURL()));
		app = manager.resolve(app);
		DeploymentMetadata depMeta = app.getDeploymentMetadata();
		List<DeploymentContent> provision = depMeta.getApplicationProvisionBundles();
		Collection<DeploymentContent> useBundles = depMeta.getDeployedUseBundle();
		Collection<DeploymentContent> appContent = depMeta.getApplicationDeploymentContents();
		// We cannot be sure whether there are two or three provision bundles pulled in by Felix OBR as there is an outstanding defect
		// https://issues.apache.org/jira/browse/FELIX-2672
		// The workaround is to check we get the two bundles we are looking for, instead of insisting on just having two bundles.

		List<String> provisionBundleSymbolicNames = new ArrayList<String>();
		for (DeploymentContent dep : provision) {
			provisionBundleSymbolicNames.add(dep.getContentName());
		}
		String provision_bundle1 = "org.apache.commons.lang";
		String provision_bundle2 = "twitter4j";
		assertTrue("Bundle " + provision_bundle1 + " not found.", provisionBundleSymbolicNames.contains(provision_bundle1));
		assertTrue("Bundle " + provision_bundle2 + " not found.", provisionBundleSymbolicNames.contains(provision_bundle2));
		assertEquals(useBundles.toString(), 0, useBundles.size());
		assertEquals(appContent.toString(), 1, appContent.size());
		AriesApplicationContext ctx = manager.install(app);
		ctx.start();
	}

	private URL getRepoUrl(String repo) throws IOException,
			MalformedURLException {
		File repoFile = File.createTempFile("twitterRepo", "xml");
		FileWriter writer = new FileWriter(repoFile);
		writer.write(repo);
		writer.close();
		return repoFile.toURI().toURL();
	}

	private void deleteRepos() {
		Repository[] repos = repositoryAdmin.listRepositories();
		for (Repository repo : repos) {
			repositoryAdmin.removeRepository(repo.getURI());
		}
	}

	private String getRepoContent(String path) throws IOException {
		StringBuilder repositoryXML = new StringBuilder();
		InputStream resourceAsStream = this.getClass().getResourceAsStream(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
		String line;
		while ((line = reader.readLine()) != null) {
			repositoryXML.append(line);
			repositoryXML.append("\r\n");
		}
		return repositoryXML.toString();
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
				mavenBundle("org.osgi", "org.osgi.compendium").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.api").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.management").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.default.local.platform").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.obr").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller").versionAsInProject(),
				mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository").versionAsInProject(),
				mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces").versionAsInProject(),
				mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
				mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint").versionAsInProject(),
				mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),
				mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
				mavenBundle("org.apache.aries.samples.twitter", "org.apache.aries.samples.twitter.twitter4j").versionAsInProject()

				// For debugging
				//vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5010"),
				);
	}
}
