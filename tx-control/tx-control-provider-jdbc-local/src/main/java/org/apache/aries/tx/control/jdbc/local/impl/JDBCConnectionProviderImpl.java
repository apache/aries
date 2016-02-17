package org.apache.aries.tx.control.jdbc.local.impl;

import java.sql.Connection;
import java.util.UUID;

import javax.sql.DataSource;

import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;

public class JDBCConnectionProviderImpl implements JDBCConnectionProvider {

	private final UUID			uuid	= UUID.randomUUID();

	private final DataSource dataSource;
	
	public JDBCConnectionProviderImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Connection getResource(TransactionControl txControl)
			throws TransactionException {
		return new TxContextBindingConnection(txControl, dataSource , uuid);
	}

}
