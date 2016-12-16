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
package org.apache.aries.tx.control.jdbc.xa.impl;

import static org.mockito.Mockito.times;
import static org.osgi.service.transaction.control.TransactionStatus.ACTIVE;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.jdbc.common.impl.AbstractJDBCConnectionProvider;
import org.apache.aries.tx.control.jdbc.xa.connection.impl.XADataSourceMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;

@RunWith(MockitoJUnitRunner.class)
public class XAEnabledTxContextBindingConnectionTest {

	@Mock
	TransactionControl control;
	
	@Mock
	TransactionContext context;
	
	@Mock
	DataSource dataSource;
	
	@Mock
	XADataSource xaDataSource;

	@Mock
	XAConnection xaMock;
	
	@Mock
	XAResource xaResource;
	
	@Mock
	Connection rawConnection;
	
	Map<Object, Object> variables = new HashMap<>();
	
	UUID id = UUID.randomUUID();
	
	
	AbstractJDBCConnectionProvider localProvider;
	AbstractJDBCConnectionProvider xaProvider;
	XAEnabledTxContextBindingConnection localConn;
	XAEnabledTxContextBindingConnection xaConn;
	
	@Before
	public void setUp() throws SQLException {
		Mockito.when(dataSource.getConnection()).thenReturn(rawConnection).thenReturn(null);
		
		Mockito.doAnswer(i -> variables.put(i.getArguments()[0], i.getArguments()[1]))
			.when(context).putScopedValue(Mockito.any(), Mockito.any());
		Mockito.when(context.getScopedValue(Mockito.any()))
			.thenAnswer(i -> variables.get(i.getArguments()[0]));
		
		Mockito.when(xaDataSource.getXAConnection()).thenReturn(xaMock);
		Mockito.when(xaMock.getConnection()).thenReturn(rawConnection);
		Mockito.when(xaMock.getXAResource()).thenReturn(xaResource);
		
		localProvider = new JDBCConnectionProviderImpl(dataSource, false, true, null);
		xaProvider = new JDBCConnectionProviderImpl(new XADataSourceMapper(xaDataSource), 
				true, false, null);
		
		localConn = new XAEnabledTxContextBindingConnection(control, localProvider, 
				id, false, true, null);
		xaConn = new XAEnabledTxContextBindingConnection(control, xaProvider, 
				id, true, false, null);
	}
	
	private void setupNoTransaction() {
		Mockito.when(control.getCurrentContext()).thenReturn(context);
		Mockito.when(context.getTransactionStatus()).thenReturn(NO_TRANSACTION);
	}

	private void setupLocalTransaction() {
		Mockito.when(control.getCurrentContext()).thenReturn(context);
		Mockito.when(context.supportsLocal()).thenReturn(true);
		Mockito.when(context.getTransactionStatus()).thenReturn(ACTIVE);
	}

	private void setupXATransaction() {
		Mockito.when(control.getCurrentContext()).thenReturn(context);
		Mockito.when(context.supportsXA()).thenReturn(true);
		Mockito.when(context.getTransactionStatus()).thenReturn(ACTIVE);
	}
	
	
	@Test(expected=TransactionException.class)
	public void testUnscopedLocal() throws SQLException {
		localConn.isValid(500);
	}

	@Test(expected=TransactionException.class)
	public void testUnscopedXA() throws SQLException {
		xaConn.isValid(500);
	}

	@Test
	public void testNoTransaction() throws SQLException {
		setupNoTransaction();
		
		localConn.isValid(500);
		localConn.isValid(500);
		
		Mockito.verify(rawConnection, times(2)).isValid(500);
		Mockito.verify(context, times(0)).registerLocalResource(Mockito.any());
		
		Mockito.verify(context).postCompletion(Mockito.any());
	}

	@Test
	public void testNoTransactionXA() throws SQLException {
		setupNoTransaction();
		
		xaConn.isValid(500);
		xaConn.isValid(500);
		
		Mockito.verify(rawConnection, times(2)).isValid(500);
		Mockito.verify(context, times(0)).registerLocalResource(Mockito.any());
		
		Mockito.verify(context).postCompletion(Mockito.any());
	}

	@Test
	public void testLocalTransactionCommit() throws SQLException {
		setupLocalTransaction();
		
		localConn.isValid(500);
		localConn.isValid(500);
		
		ArgumentCaptor<LocalResource> captor = ArgumentCaptor.forClass(LocalResource.class);

		Mockito.verify(rawConnection, times(2)).isValid(500);
		Mockito.verify(context).registerLocalResource(captor.capture());
		
		Mockito.verify(context).postCompletion(Mockito.any());
		
		captor.getValue().commit();
		
		Mockito.verify(rawConnection).commit();
	}

	@Test
	public void testLocalTransactionRollback() throws SQLException {
		setupLocalTransaction();
		
		localConn.isValid(500);
		localConn.isValid(500);
		
		ArgumentCaptor<LocalResource> captor = ArgumentCaptor.forClass(LocalResource.class);
		
		Mockito.verify(rawConnection, times(2)).isValid(500);
		Mockito.verify(context).registerLocalResource(captor.capture());
		
		Mockito.verify(context).postCompletion(Mockito.any());
		
		captor.getValue().rollback();
		
		Mockito.verify(rawConnection).rollback();
	}

	@Test(expected=TransactionException.class)
	public void testLocalTransactionNoLocal() throws SQLException {
		setupLocalTransaction();
		
		Mockito.when(context.supportsLocal()).thenReturn(false);
		localConn.isValid(500);
	}

	@Test(expected=TransactionException.class)
	public void testLocalResourceProviderClosed() throws SQLException {
		setupLocalTransaction();
		
		localProvider.close();
		localConn.isValid(500);
	}
	
	@Test(expected=TransactionException.class)
	public void testLocalConnWithXATransaction() throws SQLException {
		setupXATransaction();
		
		localConn.isValid(500);
	}

	@Test
	public void testXATransactionCommit() throws SQLException {
		setupXATransaction();
		
		xaConn.isValid(500);
		xaConn.isValid(500);
		
		
		Mockito.verify(rawConnection, times(2)).isValid(500);
		Mockito.verify(context).registerXAResource(xaResource, null);
		
		Mockito.verify(context).postCompletion(Mockito.any());
		
		Mockito.verify(rawConnection, times(0)).commit();
	}
	
	@Test
	public void testXATransactionRollback() throws SQLException {
		setupXATransaction();
		
		xaConn.isValid(500);
		xaConn.isValid(500);
		
		Mockito.verify(rawConnection, times(2)).isValid(500);
		Mockito.verify(context).registerXAResource(xaResource, null);
		
		Mockito.verify(context).postCompletion(Mockito.any());
		
		Mockito.verify(rawConnection, times(0)).rollback();
	}
	
	@Test(expected=TransactionException.class)
	public void testXAConnTransactionWithLocal() throws SQLException {
		setupLocalTransaction();
		
		xaConn.isValid(500);
	}

	@Test(expected=TransactionException.class)
	public void testRemoteResourceProviderClosed() throws SQLException {
		setupXATransaction();
		
		xaProvider.close();
		
		xaConn.isValid(500);
	}

}
