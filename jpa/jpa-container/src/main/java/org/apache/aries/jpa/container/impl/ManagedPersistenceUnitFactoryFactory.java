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

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactory;
import org.apache.aries.jpa.container.unit.impl.ManagedPersistenceUnitInfoFactoryImpl;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates either the default or a customer MPUF.
 * Loads the jpa container properties to determine if a custom MPUF is defined.
 */
public class ManagedPersistenceUnitFactoryFactory {
    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container");

    @SuppressWarnings("unchecked")
    public static ManagedPersistenceUnitInfoFactory create(Bundle bundle) {
        Properties config = new Properties();
        URL u = bundle.getResource(ManagedPersistenceUnitInfoFactory.ARIES_JPA_CONTAINER_PROPERTIES);

        if (u != null) {
            if (_logger.isInfoEnabled())
                _logger.info(NLS.MESSAGES
                    .getMessage("aries.jpa.config.file.found",
                                ManagedPersistenceUnitInfoFactory.ARIES_JPA_CONTAINER_PROPERTIES,
                                bundle.getSymbolicName(), bundle.getVersion(), config));
            try {
                config.load(u.openStream());
            } catch (IOException e) {
                _logger.error(NLS.MESSAGES
                    .getMessage("aries.jpa.config.file.read.error",
                                ManagedPersistenceUnitInfoFactory.ARIES_JPA_CONTAINER_PROPERTIES,
                                bundle.getSymbolicName(), bundle.getVersion()), e);
            }
        } else {
            if (_logger.isInfoEnabled())
                _logger.info(NLS.MESSAGES
                    .getMessage("aries.jpa.config.file.not.found",
                                ManagedPersistenceUnitInfoFactory.ARIES_JPA_CONTAINER_PROPERTIES,
                                bundle.getSymbolicName(), bundle.getVersion(), config));
        }
        // Create the pluggable ManagedPersistenceUnitInfoFactory
        String className = config.getProperty(ManagedPersistenceUnitInfoFactory.DEFAULT_PU_INFO_FACTORY_KEY);

        if (className != null) {
            try {
                Class<? extends ManagedPersistenceUnitInfoFactory> clazz = (Class<? extends ManagedPersistenceUnitInfoFactory>)bundle
                    .loadClass(className);
                return clazz.newInstance();
            } catch (Exception e) {
                _logger.error(NLS.MESSAGES.getMessage("unable.to.create.mpuif", className), e);
            }
        }

        return new ManagedPersistenceUnitInfoFactoryImpl();
    }
}
