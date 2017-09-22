package org.apache.aries.tx.control.jpa.xa.impl;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class ScopedConnectionDataSource implements DataSource {

	private final Connection scoped;
	
	public ScopedConnectionDataSource(Connection scoped) {
		this.scoped = scoped;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return null;
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		// A no-op

	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		// A no-op
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return Logger.getGlobal();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(scoped)) {
			return (T) scoped;
		} else {
			return scoped.unwrap(iface);
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(scoped) || scoped.isWrapperFor(iface);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return scoped;
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return scoped;
	}

}
