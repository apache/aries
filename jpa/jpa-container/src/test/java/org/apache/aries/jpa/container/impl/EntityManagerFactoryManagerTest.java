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

import java.util.Collections;
import java.util.Hashtable;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.apache.aries.jpa.container.parsing.impl.PersistenceUnitImpl;
import org.apache.aries.jpa.container.unit.impl.ManagedPersistenceUnitInfoImpl;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EntityManagerFactoryManagerTest {

    @Test
    public void testJndiSimple() {
        Bundle persistenceBundle = Skeleton.newMock(new BundleMock("scooby.doo", new Hashtable<String, Object>()), Bundle.class);

        PersistenceUnitImpl unit = new PersistenceUnitImpl(persistenceBundle, "persistentUnitName", null, "1.0");
        unit.setJtaDataSource("osgi:service/jdbc/DataSource");

        ManagedPersistenceUnitInfo mpuInfo = new ManagedPersistenceUnitInfoImpl(persistenceBundle, unit, null);
        EntityManagerFactoryManager emfManager = new EntityManagerFactoryManager(null, persistenceBundle);
        emfManager.manage(Collections.<ParsedPersistenceUnit>singletonList(unit), null, Collections.singletonList(mpuInfo));

        assertFalse(emfManager.availableJndiService("persistentUnitName"));

        ServiceReference dataSourceRef = Skeleton.newMock(ServiceReference.class);
        Skeleton.getSkeleton(dataSourceRef).setReturnValue(new MethodCall(ServiceReference.class, "getProperty", "osgi.jdbc.driver.class"), null);
        Skeleton.getSkeleton(dataSourceRef).setReturnValue(new MethodCall(ServiceReference.class, "getProperty", "osgi.jndi.service.name"), "jdbc/DataSource");
        emfManager.addingService(dataSourceRef);
        assertTrue(emfManager.availableJndiService("persistentUnitName"));
    }

    @Test
    public void testJndiExtended() {
        Bundle persistenceBundle = Skeleton.newMock(new BundleMock("scooby.doo", new Hashtable<String, Object>()), Bundle.class);

        PersistenceUnitImpl unit = new PersistenceUnitImpl(persistenceBundle, "persistentUnitName", null, "1.0");
        unit.setJtaDataSource("osgi:service/javax.sql.DataSource/(osgi.jndi.service.name=jdbc/DataSource)");

        ManagedPersistenceUnitInfo mpuInfo = new ManagedPersistenceUnitInfoImpl(persistenceBundle, unit, null);
        EntityManagerFactoryManager emfManager = new EntityManagerFactoryManager(null, persistenceBundle);
        emfManager.manage(Collections.<ParsedPersistenceUnit>singletonList(unit), null, Collections.singletonList(mpuInfo));

        assertFalse(emfManager.availableJndiService("persistentUnitName"));

        ServiceReference dataSourceRef = Skeleton.newMock(ServiceReference.class);
        Skeleton.getSkeleton(dataSourceRef).setReturnValue(new MethodCall(ServiceReference.class, "getProperty", "objectClass"), "javax.sql.DataSource");
        Skeleton.getSkeleton(dataSourceRef).setReturnValue(new MethodCall(ServiceReference.class, "getProperty", "osgi.jdbc.driver.class"), null);
        Skeleton.getSkeleton(dataSourceRef).setReturnValue(new MethodCall(ServiceReference.class, "getProperty", "osgi.jndi.service.name"), "jdbc/DataSource");
        emfManager.addingService(dataSourceRef);
        assertTrue(emfManager.availableJndiService("persistentUnitName"));
    }
}
