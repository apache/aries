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

import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

public class MockRepositoryAdminImpl implements RepositoryAdmin {

    List<Repository> repos = new ArrayList<Repository>();
    public MockRepositoryAdminImpl() {
        
    }
    public Repository addRepository(URL arg0) throws Exception {
        return null;
        
    }

    public Resource[] discoverResources(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public Resource getResource(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public Repository[] listRepositories() {
        return (Repository[]) repos.toArray();
    }

    public boolean removeRepository(URL arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    public Resolver resolver() {
        // TODO Auto-generated method stub
        return null;
    }

}
