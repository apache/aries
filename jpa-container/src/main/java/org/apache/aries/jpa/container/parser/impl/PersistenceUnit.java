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
package org.apache.aries.jpa.container.parser.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.aries.jpa.container.weaving.impl.TransformerRegistry;
import org.apache.aries.jpa.container.weaving.impl.TransformerRegistrySingleton;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.packageadmin.PackageAdmin;

public class PersistenceUnit implements PersistenceUnitInfo {

    private Bundle bundle;
    private ClassLoader classLoader;
    private Set<String> classNames;
    private boolean excludeUnlisted;
    private DataSource jtaDataSource;
    private String jtaDataSourceName;
    private DataSource nonJtaDataSource;
    private String nonJtaDataSourceName;
    private String persistenceProviderClassName;
    private String persistenceUnitName;
    private String persistenceXMLSchemaVersion;
    private Properties props;
    private SharedCacheMode sharedCacheMode = SharedCacheMode.UNSPECIFIED;
    private PersistenceUnitTransactionType transactionType;
    private ValidationMode validationMode = ValidationMode.NONE;

    public PersistenceUnit(Bundle bundle, String persistenceUnitName,
                           PersistenceUnitTransactionType transactionType) {
        this.bundle = bundle;
        this.persistenceUnitName = persistenceUnitName;
        this.transactionType = transactionType;
        this.props = new Properties();
        this.classLoader = bundle.adapt(BundleWiring.class).getClassLoader();
        this.classNames = new HashSet<>();
    }

    public void addClassName(String className) {
        this.classNames.add(className);
    }

    public void addProperty(String name, String value) {
        props.put(name, value);
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
        TransformerRegistry reg = TransformerRegistrySingleton.get();
        reg.addTransformer(bundle, transformer);
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return this.excludeUnlisted;
    }

    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<URL> getJarFileUrls() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public DataSource getJtaDataSource() {
        return this.jtaDataSource;
    }

    public String getJtaDataSourceName() {
        return jtaDataSourceName;
    }

    @Override
    public List<String> getManagedClassNames() {
        ArrayList<String> names = new ArrayList<>();
        names.addAll(classNames);
        return names;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getMappingFileNames() {
        return Collections.EMPTY_LIST;
    }

    public String getName() {
        return persistenceUnitName;
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return new TempBundleDelegatingClassLoader(bundle, classLoader);
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return this.nonJtaDataSource;
    }

    public String getNonJtaDataSourceName() {
        return nonJtaDataSourceName;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return this.persistenceProviderClassName;
    }

    @Override
    public String getPersistenceUnitName() {
        return this.persistenceUnitName;
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return bundle.getResource("/");
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return this.persistenceXMLSchemaVersion;
    }

    @Override
    public Properties getProperties() {
        return this.props;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return this.sharedCacheMode;
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public ValidationMode getValidationMode() {
        return this.validationMode;
    }

    public boolean isExcludeUnlisted() {
        return excludeUnlisted;
    }

    public void setExcludeUnlisted(boolean excludeUnlisted) {
        this.excludeUnlisted = excludeUnlisted;
    }

    public void setJtaDataSource(DataSource jtaDataSource) {
        this.jtaDataSource = jtaDataSource;
    }

    public void setJtaDataSourceName(String jtaDataSourceName) {
        this.jtaDataSourceName = jtaDataSourceName;
    }

    public void setNonJtaDataSource(DataSource nonJtaDataSource) {
        this.nonJtaDataSource = nonJtaDataSource;
    }

    public void setNonJtaDataSourceName(String nonJtaDataSourceName) {
        this.nonJtaDataSourceName = nonJtaDataSourceName;
    }

    public void setProviderClassName(String providerClassName) {
        this.persistenceProviderClassName = providerClassName;
    }

    public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
        this.sharedCacheMode = sharedCacheMode;
    }

    public void setValidationMode(ValidationMode validationMode) {
        this.validationMode = validationMode;
    }

    public void addAnnotated(PackageAdmin packageAdmin) {
        if (!excludeUnlistedClasses()) {
            Collection<String> detected = JPAAnnotationScanner.findJPAAnnotatedClasses(bundle);
            for (String name : detected) {
                addClassName(name);
            }
        }
    }
}
