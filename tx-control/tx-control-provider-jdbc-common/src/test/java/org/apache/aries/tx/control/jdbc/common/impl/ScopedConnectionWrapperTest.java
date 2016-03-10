package org.apache.aries.tx.control.jdbc.common.impl;

import java.awt.List;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.Executor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("resource")
public class ScopedConnectionWrapperTest {

	@Mock
	Connection conn;
	
	@Test
	public void testUnwrap() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.unwrap(List.class);
		
		Mockito.verify(conn).unwrap(List.class);
	}

	@Test
	public void testIsWrapperFor() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.isWrapperFor(List.class);
		
		Mockito.verify(conn).isWrapperFor(List.class);
	}

	@Test
	public void testCreateStatement() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.createStatement();
		
		Mockito.verify(conn).createStatement();
	}

	@Test
	public void testPrepareStatement() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.prepareStatement("foo");
		
		Mockito.verify(conn).prepareStatement("foo");
	}

	@Test
	public void testPrepareCall() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.prepareCall("foo");
		
		Mockito.verify(conn).prepareCall("foo");
	}

	@Test
	public void testNativeSQL() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.nativeSQL("foo");
		
		Mockito.verify(conn).nativeSQL("foo");
	}

	@Test
	public void testAutoCommit() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.setAutoCommit(true);
		
		Mockito.verify(conn).setAutoCommit(true);
	}

	@Test
	public void testGetAutoCommit() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.getAutoCommit();
		
		Mockito.verify(conn).getAutoCommit();
	}

	@Test
	public void testCommit() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.commit();
		
		Mockito.verify(conn).commit();
	}

	@Test
	public void testRollback() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.rollback();
		
		Mockito.verify(conn).rollback();
	}

	@Test
	public void testClose() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.close();
		
		Mockito.verify(conn, Mockito.times(0)).close();
	}

	@Test
	public void testIsClosed() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.isClosed();
		
		Mockito.verify(conn).isClosed();
	}
	
	@Test
	public void testAbort() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.abort(x -> {});
		
		Mockito.verify(conn, Mockito.times(0)).abort(Mockito.any(Executor.class));
	}
	
	@Test
	public void testTransactionIsolation() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.setTransactionIsolation(1);
		
		Mockito.verify(conn).setTransactionIsolation(1);
	}

	@Test
	public void testGetTransactionIsolation() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.getTransactionIsolation();
		
		Mockito.verify(conn).getTransactionIsolation();
	}

	@Test
	public void testSetSavepoint() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.setSavepoint();
		
		Mockito.verify(conn).setSavepoint();
	}

	@Test
	public void testSetSavepointString() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.setSavepoint("foo");
		
		Mockito.verify(conn).setSavepoint("foo");
	}

	@Test
	public void testRollbackSavepoint() throws SQLException {
		Savepoint s = Mockito.mock(Savepoint.class);
		
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.rollback(s);
		
		Mockito.verify(conn).rollback(s);
	}

	@Test
	public void testReleaseSavepoint() throws SQLException {
		Savepoint s = Mockito.mock(Savepoint.class);
		
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.releaseSavepoint(s);
		
		Mockito.verify(conn).releaseSavepoint(s);
	}

	@Test
	public void testSetReadOnly() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.setReadOnly(true);
		
		Mockito.verify(conn).setReadOnly(true);
	}

	@Test
	public void testIsReadOnly() throws SQLException {
		Connection wrapped = new ScopedConnectionWrapper(conn);
		wrapped.isReadOnly();
		
		Mockito.verify(conn).isReadOnly();
	}

	
	
}
