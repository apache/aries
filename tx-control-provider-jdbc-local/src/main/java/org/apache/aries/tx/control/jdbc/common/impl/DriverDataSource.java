package org.apache.aries.tx.control.jdbc.common.impl;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class DriverDataSource implements DataSource {

	private final Driver driver;
	private final String jdbcURL;
	private final Properties properties;

	public DriverDataSource(Driver driver, String jdbcURL, Properties properties) {
		this.driver = driver;
		this.jdbcURL = jdbcURL;
		this.properties = properties;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		throw new SQLFeatureNotSupportedException("Driver based JDBC does not support log writing");
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new SQLFeatureNotSupportedException("Driver based JDBC does not support log writing");
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		throw new SQLFeatureNotSupportedException("Driver based JDBC does not support login timeouts");
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		throw new SQLFeatureNotSupportedException("Driver based JDBC does not support login timeouts");
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("Driver based JDBC does not support log writing");
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new SQLFeatureNotSupportedException("Driver based JDBC does not support unwrapping");
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return driver.connect(jdbcURL, properties);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection();
	}

}
