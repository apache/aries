package org.apache.aries.tx.control.jdbc.common.impl;

import java.awt.List;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import org.apache.aries.tx.control.jdbc.common.impl.TxConnectionWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.transaction.control.TransactionException;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("resource")
public class TxConnectionWrapperTest {

	@Mock
	Connection conn;
	
	@Test
	public void testUnwrap() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.unwrap(List.class);
		
		Mockito.verify(conn).unwrap(List.class);
	}

	@Test
	public void testIsWrapperFor() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.isWrapperFor(List.class);
		
		Mockito.verify(conn).isWrapperFor(List.class);
	}

	@Test
	public void testCreateStatement() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.createStatement();
		
		Mockito.verify(conn).createStatement();
	}

	@Test
	public void testPrepareStatement() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.prepareStatement("foo");
		
		Mockito.verify(conn).prepareStatement("foo");
	}

	@Test
	public void testPrepareCall() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.prepareCall("foo");
		
		Mockito.verify(conn).prepareCall("foo");
	}

	@Test
	public void testNativeSQL() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.nativeSQL("foo");
		
		Mockito.verify(conn).nativeSQL("foo");
	}

	@Test(expected=TransactionException.class)
	public void testAutoCommit() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.setAutoCommit(true);
	}

	@Test
	public void testGetAutoCommit() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.getAutoCommit();
		
		Mockito.verify(conn).getAutoCommit();
	}

	@Test(expected=TransactionException.class)
	public void testCommit() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.commit();
	}

	@Test(expected=TransactionException.class)
	public void testRollback() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.rollback();
	}

	@Test
	public void testClose() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.close();
		
		Mockito.verify(conn, Mockito.times(0)).close();
	}

	@Test
	public void testIsClosed() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.isClosed();
		
		Mockito.verify(conn).isClosed();
	}
	
	@Test
	public void testAbort() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.abort(x -> {});
		
		Mockito.verify(conn, Mockito.times(0)).abort(Mockito.any(Executor.class));
	}
	
	@Test
	public void testTransactionIsolation() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.setTransactionIsolation(1);
		
		Mockito.verify(conn).setTransactionIsolation(1);
	}

	@Test
	public void testGetTransactionIsolation() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.getTransactionIsolation();
		
		Mockito.verify(conn).getTransactionIsolation();
	}

	@Test(expected=TransactionException.class)
	public void testSetSavepoint() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.setSavepoint();
	}

	@Test(expected=TransactionException.class)
	public void testSetSavepointString() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.setSavepoint("foo");
	}

	@Test(expected=TransactionException.class)
	public void testRollbackSavepoint() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.rollback(null);
	}

	@Test(expected=TransactionException.class)
	public void testReleaseSavepoint() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.releaseSavepoint(null);
	}

	@Test
	public void testSetReadOnly() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.setReadOnly(true);
		
		Mockito.verify(conn).setReadOnly(true);
	}

	@Test
	public void testIsReadOnly() throws SQLException {
		Connection wrapped = new TxConnectionWrapper(conn);
		wrapped.isReadOnly();
		
		Mockito.verify(conn).isReadOnly();
	}

	
	
}
