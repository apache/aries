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

import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.aries.jpa.container.impl.DataSourceTracker;
import org.apache.aries.jpa.container.parser.impl.PersistenceUnit;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

public class DataSourceTrackerTest {
    
    @Test
    public void testCreateFilterFull() throws InvalidSyntaxException {
        PersistenceUnit punit = mock(PersistenceUnit.class);
        when(punit.getJtaDataSourceName()).thenReturn("osgi:service/javax.sql.DataSource/(osgi.jndi.service.name=tasklist)");
        when(punit.getTransactionType()).thenReturn(PersistenceUnitTransactionType.JTA);
        BundleContext context = mock(BundleContext.class);
        
        DataSourceTracker.createFilter(context, punit);

        verify(context, atLeastOnce()).createFilter(Mockito.eq("(&(objectClass=javax.sql.DataSource)(osgi.jndi.service.name=tasklist))"));
    }
    
    @Test
    public void testCreateFilterSimple() throws InvalidSyntaxException {
        PersistenceUnit punit = mock(PersistenceUnit.class);
        when(punit.getJtaDataSourceName()).thenReturn("tasklist");
        when(punit.getTransactionType()).thenReturn(PersistenceUnitTransactionType.JTA);
        BundleContext context = mock(BundleContext.class);
        
        DataSourceTracker.createFilter(context, punit);

        verify(context, atLeastOnce()).createFilter(Mockito.eq("(&(objectClass=javax.sql.DataSource)(osgi.jndi.service.name=tasklist))"));
    }
}
