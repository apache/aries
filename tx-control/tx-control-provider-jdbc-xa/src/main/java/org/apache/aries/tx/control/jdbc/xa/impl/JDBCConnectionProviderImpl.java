package org.apache.aries.tx.control.jdbc.xa.impl;

import java.sql.Connection;
import java.util.UUID;

import javax.sql.DataSource;

import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;

public class JDBCConnectionProviderImpl implements JDBCConnectionProvider {

	private final UUID			uuid	= UUID.randomUUID();

	private final DataSource dataSource;
	
	private final boolean xaEnabled;
	
	private final boolean localEnabled;
	
	public JDBCConnectionProviderImpl(DataSource dataSource, boolean xaEnabled,
			boolean localEnabled) {
		this.dataSource = dataSource;
		this.xaEnabled = xaEnabled;
		this.localEnabled = localEnabled;
	}

	@Override
	public Connection getResource(TransactionControl txControl)
			throws TransactionException {
		return new XAEnabledTxContextBindingConnection(txControl, dataSource , uuid,
				xaEnabled, localEnabled);
	}

}
