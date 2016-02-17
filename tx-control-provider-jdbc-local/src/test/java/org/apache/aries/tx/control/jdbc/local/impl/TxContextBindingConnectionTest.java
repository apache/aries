package org.apache.aries.tx.control.jdbc.local.impl;

import static org.mockito.Mockito.times;
import static org.osgi.service.transaction.control.TransactionStatus.ACTIVE;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

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
public class TxContextBindingConnectionTest {

	@Mock
	TransactionControl control;
	
	@Mock
	TransactionContext context;
	
	@Mock
	DataSource dataSource;
	
	@Mock
	Connection rawConnection;
	
	Map<Object, Object> variables = new HashMap<>();
	
	UUID id = UUID.randomUUID();
	
	TxContextBindingConnection conn;
	
	@Before
	public void setUp() throws SQLException {
		Mockito.when(dataSource.getConnection()).thenReturn(rawConnection).thenReturn(null);
		
		Mockito.doAnswer(i -> variables.put(i.getArguments()[0], i.getArguments()[1]))
			.when(context).putScopedValue(Mockito.any(), Mockito.any());
		Mockito.when(context.getScopedValue(Mockito.any()))
			.thenAnswer(i -> variables.get(i.getArguments()[0]));
		
		conn = new TxContextBindingConnection(control, dataSource, id);
	}
	
	private void setupNoTransaction() {
		Mockito.when(control.getCurrentContext()).thenReturn(context);
		Mockito.when(context.getTransactionStatus()).thenReturn(NO_TRANSACTION);
	}

	private void setupActiveTransaction() {
		Mockito.when(control.getCurrentContext()).thenReturn(context);
		Mockito.when(context.supportsLocal()).thenReturn(true);
		Mockito.when(context.getTransactionStatus()).thenReturn(ACTIVE);
	}
	
	
	@Test(expected=TransactionException.class)
	public void testUnscoped() throws SQLException {
		conn.isValid(500);
	}

	@Test
	public void testNoTransaction() throws SQLException {
		setupNoTransaction();
		
		conn.isValid(500);
		conn.isValid(500);
		
		Mockito.verify(rawConnection, times(2)).isValid(500);
		Mockito.verify(context, times(0)).registerLocalResource(Mockito.any());
		
		Mockito.verify(context).postCompletion(Mockito.any());
	}

	@Test
	public void testActiveTransactionCommit() throws SQLException {
		setupActiveTransaction();
		
		conn.isValid(500);
		conn.isValid(500);
		
		ArgumentCaptor<LocalResource> captor = ArgumentCaptor.forClass(LocalResource.class);

		Mockito.verify(rawConnection, times(2)).isValid(500);
		Mockito.verify(context).registerLocalResource(captor.capture());
		
		Mockito.verify(context).postCompletion(Mockito.any());
		
		captor.getValue().commit();
		
		Mockito.verify(rawConnection).commit();
	}

	@Test
	public void testActiveTransactionRollback() throws SQLException {
		setupActiveTransaction();
		
		conn.isValid(500);
		conn.isValid(500);
		
		ArgumentCaptor<LocalResource> captor = ArgumentCaptor.forClass(LocalResource.class);
		
		Mockito.verify(rawConnection, times(2)).isValid(500);
		Mockito.verify(context).registerLocalResource(captor.capture());
		
		Mockito.verify(context).postCompletion(Mockito.any());
		
		captor.getValue().rollback();
		
		Mockito.verify(rawConnection).rollback();
	}

	@Test(expected=TransactionException.class)
	public void testActiveTransactionNoLocal() throws SQLException {
		setupActiveTransaction();
		
		Mockito.when(context.supportsLocal()).thenReturn(false);
		conn.isValid(500);
	}

}
