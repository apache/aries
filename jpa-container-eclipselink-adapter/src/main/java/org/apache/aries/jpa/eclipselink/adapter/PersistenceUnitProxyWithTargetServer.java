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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.eclipselink.adapter;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.aries.jpa.eclipselink.adapter.platform.OSGiTSServer;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Wrapper {@link PersistenceUnitInfo} object that adds the eclipselink.target-server setting (if not present)
 * and makes sure we can load {@link OSGiTSServer} from the unit's classloader.
 */
public class PersistenceUnitProxyWithTargetServer implements PersistenceUnitInfo {
  private final PersistenceUnitInfo delegate;
    private final ClassLoader unionClassLoader; 
    
    public PersistenceUnitProxyWithTargetServer(PersistenceUnitInfo info, Bundle b, Map<String, Object> arg1) {
        delegate = info;
        unionClassLoader = new UnionClassLoader(delegate.getClassLoader(), b, 
            FrameworkUtil.getBundle(getClass()), arg1);
    }

    @Override
    public void addTransformer(ClassTransformer arg0) {
        delegate.addTransformer(arg0);
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return delegate.excludeUnlistedClasses();
    }

    @Override
    public ClassLoader getClassLoader() {
        return unionClassLoader;
    }

    @Override
    public List<URL> getJarFileUrls() {
        return delegate.getJarFileUrls();
    }

    @Override
    public DataSource getJtaDataSource() {
        return delegate.getJtaDataSource();
    }

    @Override
    public List<String> getManagedClassNames() {
        return delegate.getManagedClassNames();
    }

    @Override
    public List<String> getMappingFileNames() {
        return delegate.getMappingFileNames();
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return delegate.getNewTempClassLoader();
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return delegate.getNonJtaDataSource();
    }

    @Override
    public String getPersistenceProviderClassName() {
        return delegate.getPersistenceProviderClassName();
    }

    @Override
    public String getPersistenceUnitName() {
        return delegate.getPersistenceUnitName();
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return delegate.getPersistenceUnitRootUrl();
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return delegate.getPersistenceXMLSchemaVersion();
    }

    @Override
    public Properties getProperties() {
        Properties props = delegate.getProperties();
        
        if (props == null) {
            props = new Properties();
        }
        
        if (!props.containsKey("eclipselink.target-server")) {
            props.put("eclipselink.target-server", 
                "org.apache.aries.jpa.eclipselink.adapter.platform.OSGiTSServer");
        }
        
        return props;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return delegate.getSharedCacheMode();
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return delegate.getTransactionType();
    }

    @Override
    public ValidationMode getValidationMode() {
        return delegate.getValidationMode();
    }
}
  