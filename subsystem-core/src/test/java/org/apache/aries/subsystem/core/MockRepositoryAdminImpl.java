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
