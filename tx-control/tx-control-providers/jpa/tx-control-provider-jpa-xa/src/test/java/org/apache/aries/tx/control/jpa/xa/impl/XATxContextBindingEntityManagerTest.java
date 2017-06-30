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
package org.apache.aries.tx.control.jpa.xa.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.withSettings;
import static org.osgi.service.transaction.control.TransactionStatus.ACTIVE;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTED;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.jdbc.xa.connection.impl.XAConnectionWrapper;
import org.apache.aries.tx.control.jpa.common.impl.AbstractJPAEntityManagerProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.TransactionStatus;

@RunWith(MockitoJUnitRunner.class)
public class XATxContextBindingEntityManagerTest {

	@Mock
	TransactionControl control;
	
	@Mock
	TransactionContext context;
	
	@Mock
	EntityManagerFactory emf;
	
	@Mock
	EntityManager rawEm;

	@Mock
	XAResource xaResource;

	Map<Object, Object> variables = new HashMap<>();
	
	UUID id = UUID.randomUUID();
	
	XATxContextBindingEntityManager em;
	
	ThreadLocal<TransactionControl> commonTxControl = new ThreadLocal<>();
	
	AbstractJPAEntityManagerProvider provider;
	
	@Before
	public void setUp() throws SQLException {
		Mockito.when(emf.createEntityManager()).thenReturn(rawEm).thenReturn(null);
		
		Mockito.doAnswer(i -> variables.put(i.getArguments()[0], i.getArguments()[1]))
			.when(context).putScopedValue(Mockito.any(), Mockito.any());
		Mockito.when(context.getScopedValue(Mockito.any()))
			.thenAnswer(i -> variables.get(i.getArguments()[0]));
		
		provider = new JPAEntityManagerProviderImpl(emf, commonTxControl, null);
		
		em = new XATxContextBindingEntityManager(control, provider, id, commonTxControl);
	}
	
	private void setupNoTransaction() {
		Mockito.when(control.getCurrentContext()).thenReturn(context);
		Mockito.when(context.getTransactionStatus()).thenReturn(NO_TRANSACTION);
	}

	private void setupActiveTransaction() {
		Mockito.when(control.getCurrentContext()).thenReturn(context);
		Mockito.when(context.supportsXA()).thenReturn(true);
		Mockito.when(context.getTransactionStatus()).thenReturn(ACTIVE);
	}
	
	
	@Test(expected=TransactionException.class)
	public void testUnscoped() throws SQLException {
		em.isOpen();
	}

	@Test
	public void testNoTransaction() throws SQLException {
		setupNoTransaction();
		
		em.isOpen();
		em.isOpen();
		
		Mockito.verify(rawEm, times(2)).isOpen();
		Mockito.verify(rawEm, times(0)).getTransaction();
		Mockito.verify(context, times(0)).registerXAResource(Mockito.any(), Mockito.anyString());
		
		checkPostCompletion(null);
	}

	private void checkPostCompletion(TransactionControl expectedAfter) {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		ArgumentCaptor<Consumer<TransactionStatus>> captor = 
				(ArgumentCaptor)ArgumentCaptor.forClass(Consumer.class);
		Mockito.verify(context).postCompletion(captor.capture());
		
		captor.getValue().accept(COMMITTED);
		
		assertEquals(expectedAfter, commonTxControl.get());
	}

	@Test
	public void testActiveTransactionStraightXAConnection() throws SQLException {
		
		Connection con = Mockito.mock(Connection.class, withSettings().extraInterfaces(XAConnection.class));
		Mockito.when(((XAConnection)con).getXAResource()).thenReturn(xaResource);
		
		Mockito.when(rawEm.unwrap(Connection.class)).thenReturn(con);
		
		setupActiveTransaction();
		
		em.isOpen();
		em.isOpen();
		
		Mockito.verify(rawEm, times(2)).isOpen();
		Mockito.verify(rawEm).joinTransaction();
		
		checkPostCompletion(null);
	}

	@Test
	public void testActiveTransactionWrappedXAConnection() throws SQLException {
		
		XAConnection con = Mockito.mock(XAConnection.class);
		Connection raw = Mockito.mock(Connection.class);
		Mockito.when(con.getXAResource()).thenReturn(xaResource);
		Mockito.when(con.getConnection()).thenReturn(raw);
		
		XAConnectionWrapper value = new XAConnectionWrapper(con);
		
		Mockito.when(rawEm.unwrap(Connection.class)).thenReturn(value);
		
		setupActiveTransaction();
		
		em.isOpen();
		em.isOpen();
		
		Mockito.verify(rawEm, times(2)).isOpen();
		Mockito.verify(rawEm).joinTransaction();
		
		checkPostCompletion(null);
	}

	@Test
	public void testActiveTransactionUnwrappableXAConnection() throws SQLException {
		
		XAConnection xaCon = Mockito.mock(XAConnection.class);
		Mockito.when(xaCon.getXAResource()).thenReturn(xaResource);
		Connection con = Mockito.mock(Connection.class);
		Mockito.when(con.unwrap(XAConnection.class)).thenReturn(xaCon);
		Mockito.when(con.isWrapperFor(XAConnection.class)).thenReturn(true);
		
		Mockito.when(rawEm.unwrap(Connection.class)).thenReturn(con);
		
		setupActiveTransaction();
		
		em.isOpen();
		em.isOpen();
		
		Mockito.verify(rawEm, times(2)).isOpen();
		Mockito.verify(rawEm).joinTransaction();
		
		checkPostCompletion(null);
	}

	@Test
	public void testActiveTransactionUnwrappableXAConnectionWrapper() throws SQLException {
		
		XAConnection xaCon = Mockito.mock(XAConnection.class);
		Mockito.when(xaCon.getXAResource()).thenReturn(xaResource);
		Connection con = Mockito.mock(Connection.class);
		XAConnectionWrapper toReturn = new XAConnectionWrapper(xaCon);
		Mockito.when(con.unwrap(XAConnectionWrapper.class)).thenReturn(toReturn);
		Mockito.when(con.isWrapperFor(XAConnectionWrapper.class)).thenReturn(true);
		
		Mockito.when(rawEm.unwrap(Connection.class)).thenReturn(con);
		
		setupActiveTransaction();
		
		em.isOpen();
		em.isOpen();
		
		Mockito.verify(rawEm, times(2)).isOpen();
		Mockito.verify(rawEm).joinTransaction();
		
		checkPostCompletion(null);
	}

	@Test(expected=TransactionException.class)
	public void testActiveTransactionNoXA() throws SQLException {
		setupActiveTransaction();
		
		Mockito.when(context.supportsXA()).thenReturn(false);
		em.isOpen();
	}

	@Test(expected=TransactionException.class)
	public void testResourceProviderClosed() throws SQLException {
		setupActiveTransaction();
		
		provider.close();
		em.isOpen();
	}

	@Test
	public void testActiveTransactionWithPreviousCommonTxControl() throws SQLException {
		
		TransactionControl previous = Mockito.mock(TransactionControl.class);
		Connection con = Mockito.mock(Connection.class, withSettings().extraInterfaces(XAConnection.class));
		Mockito.when(((XAConnection)con).getXAResource()).thenReturn(xaResource);
		
		Mockito.when(rawEm.unwrap(Connection.class)).thenReturn(con);
		
		setupActiveTransaction();
		commonTxControl.set(previous);
		
		em.isOpen();
		em.isOpen();
		
		Mockito.verify(rawEm, times(2)).isOpen();
		Mockito.verify(rawEm).joinTransaction();
		
		checkPostCompletion(previous);
	}
}
