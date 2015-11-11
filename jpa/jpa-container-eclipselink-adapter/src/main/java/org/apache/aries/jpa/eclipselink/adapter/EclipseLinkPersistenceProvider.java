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

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.osgi.framework.Bundle;

@SuppressWarnings("rawtypes")
final class EclipseLinkPersistenceProvider implements PersistenceProvider {
    private final PersistenceProvider delegate;
    private final Bundle eclipeLinkBundle;

    EclipseLinkPersistenceProvider(PersistenceProvider delegate, Bundle eclipeLinkBundle) {
        this.delegate = delegate;
        this.eclipeLinkBundle = eclipeLinkBundle;
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return delegate.getProviderUtil();
    }
    
    @Override
    public EntityManagerFactory createEntityManagerFactory(String arg0, Map arg1) {
        return delegate.createEntityManagerFactory(arg0, arg1);
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo punit, Map props) {
        return delegate.createContainerEntityManagerFactory(new PersistenceUnitProxyWithTargetServer(punit, eclipeLinkBundle), props);
    }

    @Override
    public void generateSchema(PersistenceUnitInfo punit, Map arg1) {
        delegate.generateSchema(new PersistenceUnitProxyWithTargetServer(punit, eclipeLinkBundle), arg1);
    }

    @Override
    public boolean generateSchema(String arg0, Map arg1) {
        return delegate.generateSchema(arg0, arg1);
    }
}
