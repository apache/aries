package org.apache.aries.tx.control.jdbc.xa.impl;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.sql.XADataSource;

public class XADataSourceMapper implements DataSource {

	private final XADataSource xaDataSource;
	
	public XADataSourceMapper(XADataSource xaDataSource) {
		super();
		this.xaDataSource = xaDataSource;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return xaDataSource.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		xaDataSource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		xaDataSource.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return xaDataSource.getLoginTimeout();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return xaDataSource.getParentLogger();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if(isWrapperFor(iface)) {
			return (T) xaDataSource;
		}
		throw new SQLException("This datasource is not a wrapper for " + iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface == XADataSource.class || iface.isInstance(xaDataSource);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return new XAConnectionWrapper(xaDataSource.getXAConnection());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return new XAConnectionWrapper(xaDataSource.getXAConnection(username, password));
	}

}
