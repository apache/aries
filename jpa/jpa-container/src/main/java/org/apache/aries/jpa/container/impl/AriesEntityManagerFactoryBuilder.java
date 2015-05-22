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

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.osgi.service.jpa.EntityManagerFactoryBuilder;

/**
 * FIXME We are currently not configuring a DataSource for the persistence unit.
 * It still works in the tests as the DataSource is defined in the DataSourceTracker or DSFTracker.
 * This not fully correct though.
 */
public class AriesEntityManagerFactoryBuilder implements EntityManagerFactoryBuilder {
    private static final String JAVAX_PERSISTENCE_JDBC_DRIVER = "javax.persistence.jdbc.driver";

    private PersistenceProvider provider;
    private PersistenceUnitInfo persistenceUnit;
    private String driver;

    public AriesEntityManagerFactoryBuilder(PersistenceProvider provider, PersistenceUnitInfo persistenceUnit) {
        this.provider = provider;
        this.persistenceUnit = persistenceUnit;
        this.driver = (String)persistenceUnit.getProperties().get(JAVAX_PERSISTENCE_JDBC_DRIVER);
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(Map<String, Object> props) {
        String newDriver = (String)props.get(JAVAX_PERSISTENCE_JDBC_DRIVER);
        if (driver == null) {
            driver = newDriver;
        } else if (newDriver != null && !newDriver.equals(driver)){
            throw new IllegalArgumentException("Can not rebind to a different database driver");
        }
        return provider.createContainerEntityManagerFactory(persistenceUnit, props);
    }

}
