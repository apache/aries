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
package org.apache.aries.jpa.container.impl;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.aries.jpa.container.impl.ManagedEMF;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationException;

public class PropsConfigurationTest {

    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    @Test
    public void testEmfWithoutProps() throws InvalidSyntaxException, ConfigurationException {
        PersistenceUnitInfo punit = mock(PersistenceUnitInfo.class);
        when(punit.getPersistenceUnitName()).thenReturn("test-props");
        when(punit.getPersistenceProviderClassName())
            .thenReturn("org.eclipse.persistence.jpa.PersistenceProvider");
        when(punit.getTransactionType()).thenReturn(PersistenceUnitTransactionType.JTA);
        BundleContext context = mock(BundleContext.class);
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBundleContext()).thenReturn(context);
        when(bundle.getVersion()).thenReturn(new Version("4.3.1"));
        PersistenceProvider provider = mock(PersistenceProvider.class);

        ManagedEMF emf = new ManagedEMF(context, bundle, provider, punit);
        emf.updated(null);
        emf.close();
        verify(provider, atLeastOnce()).createContainerEntityManagerFactory(Mockito.eq(punit),
                                                                            Mockito.eq((Map)null));
        verify(context, atLeastOnce()).registerService(Mockito.eq(EntityManagerFactory.class),
                                                       Mockito.any(EntityManagerFactory.class),
                                                       Mockito.any(Dictionary.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEmfWithProps() throws InvalidSyntaxException, ConfigurationException {
        PersistenceUnitInfo punit = mock(PersistenceUnitInfo.class);
        when(punit.getPersistenceUnitName()).thenReturn("test-props");
        when(punit.getPersistenceProviderClassName())
            .thenReturn("org.eclipse.persistence.jpa.PersistenceProvider");
        when(punit.getTransactionType()).thenReturn(PersistenceUnitTransactionType.JTA);
        BundleContext context = mock(BundleContext.class);
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBundleContext()).thenReturn(context);
        when(bundle.getVersion()).thenReturn(new Version("4.3.1"));
        PersistenceProvider provider = mock(PersistenceProvider.class);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        ManagedEMF emf = new ManagedEMF(context, bundle, provider, punit);
        emf.updated(null);
        emf.close();

        verify(provider, atLeastOnce()).createContainerEntityManagerFactory(Mockito.eq(punit),
                                                                            Mockito.anyMap());
        verify(context, atLeastOnce()).registerService(Mockito.eq(EntityManagerFactory.class),
                                                       Mockito.any(EntityManagerFactory.class),
                                                       Mockito.any(Dictionary.class));
    }
}
