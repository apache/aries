package org.apache.aries.tx.control.jdbc.xa.connection.impl;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.jdbc.common.impl.ConnectionWrapper;

public class XAConnectionWrapper extends ConnectionWrapper {

	private final Connection connection;
	
	private final XAResource xaResource;
	
	public XAConnectionWrapper(XAConnection xaConnection) throws SQLException {
		this.connection = xaConnection.getConnection();
		this.xaResource = xaConnection.getXAResource();
	}

	@Override
	protected Connection getDelegate() {
		return connection;
	}

	public XAResource getXaResource() {
		return xaResource;
	}
}
