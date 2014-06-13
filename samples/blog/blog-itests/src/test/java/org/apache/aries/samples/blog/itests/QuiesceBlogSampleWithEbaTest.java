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

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.quiesce.manager.QuiesceManager;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.Bundle;


public class QuiesceBlogSampleWithEbaTest extends AbstractBlogIntegrationTest {
	@Inject
	QuiesceManager quiesceMgr;

	@Test
	public void test() throws Exception {
		resolveBundles();
		MavenArtifactUrlReference eba = CoreOptions.maven()
				.groupId("org.apache.aries.samples.blog")
				.artifactId("org.apache.aries.samples.blog.jpa.eba")
				.versionAsInProject()
				.type("eba");
		AriesApplicationContext ctx = installEba(eba);

		/* Find and check all the blog sample bundles */
		Bundle bapi = assertBundleStarted("org.apache.aries.samples.blog.api");
		Bundle bweb = assertBundleStarted("org.apache.aries.samples.blog.web");
		Bundle bbiz = assertBundleStarted("org.apache.aries.samples.blog.biz");
		Bundle bper = assertBundleStarted("org.apache.aries.samples.blog.persistence.jpa");
		Bundle bds = assertBundleStarted("org.apache.aries.samples.blog.datasource");
		Bundle txs = assertBundleStarted("org.apache.aries.transaction.manager");

		assertBlogServicesStarted();
		checkBlogWebAccess();

		//So Blog is working properly, let's quiesce it, we would expect to get a JPA and a Blueprint 
		//participant
		quiesceMgr.quiesce(500, Collections.singletonList(bapi));
		Thread.sleep(1000);

		// Blog api bundle should now be stopped, but others should still be running
		assertResolved(bapi);
		assertActive(bweb);
		assertActive(bbiz);
		assertActive(bper);

		quiesceMgr.quiesce(500, Arrays.asList(bapi, bweb, bbiz, bper));
		Thread.sleep(1000);

		// All blog bundles should now be stopped
		assertResolved(bapi);
		assertResolved(bweb);
		assertResolved(bbiz);
		assertResolved(bper);

		// Check we can start them again after quiesce and everything works as before
		bapi.start();
		bweb.start();
		bbiz.start();
		bper.start();

		assertBlogServicesStarted();
		assertBlogServicesStarted();
		System.out.println("Checking if blog works again after restart");
		checkBlogWebAccess();

		ctx.stop();
		manager.uninstall(ctx);

	}

}
