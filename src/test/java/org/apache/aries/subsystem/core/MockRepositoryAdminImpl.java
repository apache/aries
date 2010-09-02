/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.subsystem.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.InvalidSyntaxException;

public class MockRepositoryAdminImpl implements RepositoryAdmin {

    List<Repository> repos = new ArrayList<Repository>();

	public Repository addRepository(String arg0) throws Exception {
		return null;
	}

	public Repository addRepository(URL arg0) throws Exception {
		return null;
	}

	public Resource[] discoverResources(String arg0)
			throws InvalidSyntaxException {
		return null;
	}

	public Resource[] discoverResources(Requirement[] arg0) {
		return null;
	}

	public DataModelHelper getHelper() {
		return null;
	}

	public Repository getLocalRepository() {
		return null;
	}

	public Repository getSystemRepository() {
		return null;
	}

	public Repository[] listRepositories() {
		return (Repository[])repos.toArray();
	}

	public boolean removeRepository(String arg0) {
		return false;
	}

	public Resolver resolver() {
		return null;
	}

	public Resolver resolver(Repository[] arg0) {
		return null;
	}
}
