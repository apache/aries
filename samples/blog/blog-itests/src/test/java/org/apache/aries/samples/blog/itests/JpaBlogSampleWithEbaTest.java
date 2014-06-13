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

import static org.ops4j.pax.exam.CoreOptions.maven;

import org.apache.aries.application.management.AriesApplicationContext;
import org.junit.Test;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

public class JpaBlogSampleWithEbaTest extends AbstractBlogIntegrationTest {

	@Test
	public void test() throws Exception {
		MavenArtifactUrlReference eba = maven()
				.groupId("org.apache.aries.samples.blog")
				.artifactId("org.apache.aries.samples.blog.jpa.eba")
				.versionAsInProject()
				.type("eba");
		AriesApplicationContext ctx = installEba(eba);

		/* Find and check all the blog sample bundles */
		assertBundleStarted("org.apache.aries.samples.blog.api");
		assertBundleStarted("org.apache.aries.samples.blog.web");
		assertBundleStarted("org.apache.aries.samples.blog.biz");
		assertBundleStarted("org.apache.aries.samples.blog.persistence.jpa");
		assertBundleStarted("org.apache.aries.samples.blog.datasource");
		assertBundleStarted("org.apache.aries.transaction.manager");

		assertBlogServicesStarted();
		checkBlogWebAccess();
		
		ctx.stop();
		manager.uninstall(ctx);
	}

}
