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

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import org.junit.Test;
import org.osgi.framework.Bundle;

public class JdbcBlogSampleTest extends AbstractBlogIntegrationTest {

    @Test
    public void test() throws Exception {
        Bundle bundleApi = context.installBundle(mavenBundle("org.apache.aries.samples.blog", "org.apache.aries.samples.blog.api").versionAsInProject().getURL());
        Bundle bundleWeb = context.installBundle(mavenBundle("org.apache.aries.samples.blog", "org.apache.aries.samples.blog.web").versionAsInProject().getURL());
        Bundle bundleBiz = context.installBundle(mavenBundle("org.apache.aries.samples.blog", "org.apache.aries.samples.blog.biz").versionAsInProject().getURL());
        Bundle bundlePersistenceJdbc = context.installBundle(mavenBundle("org.apache.aries.samples.blog", "org.apache.aries.samples.blog.persistence.jdbc").versionAsInProject().getURL());

        bundleApi.start();
        bundlePersistenceJdbc.start();
        bundleBiz.start();
        bundleWeb.start();

		/* Check that the Blog Sample bundles are present an started */
        assertBundleStarted("org.apache.aries.samples.blog.api");
        assertBundleStarted("org.apache.aries.samples.blog.web");
        assertBundleStarted("org.apache.aries.samples.blog.biz");
        assertBundleStarted("org.apache.aries.samples.blog.persistence.jdbc");

        assertBlogServicesStarted();
        checkBlogWebAccess();

        bundleWeb.uninstall();
        bundleBiz.uninstall();
        bundlePersistenceJdbc.uninstall();
        bundleApi.uninstall();
    }

}
