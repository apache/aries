package org.apache.aries.tx.control.jpa.xa.impl;

import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;
import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.jdbc.common.impl.ConnectionWrapper;
import org.apache.aries.tx.control.jdbc.common.impl.TxConnectionWrapper;
import org.apache.aries.tx.control.jdbc.xa.connection.impl.XAConnectionWrapper;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;

public class XAEnabledTxContextBindingConnection extends ConnectionWrapper {

	private final TransactionControl	txControl;
	private final UUID					resourceId;
	private final DataSource			dataSource;

	public XAEnabledTxContextBindingConnection(TransactionControl txControl,
			DataSource dataSource, UUID resourceId, boolean xaEnabled, boolean localEnabled) {
		this.txControl = txControl;
		this.dataSource = dataSource;
		this.resourceId = resourceId;
	}

	@Override
	protected final Connection getDelegate() {

		TransactionContext txContext = txControl.getCurrentContext();

		if (txContext == null) {
			throw new TransactionException("The resource " + dataSource
					+ " cannot be accessed outside of an active Transaction Context");
		}

		Connection existing = (Connection) txContext.getScopedValue(resourceId);

		if (existing != null) {
			return existing;
		}

		Connection toReturn;
		Connection toClose;

		try {
			if (txContext.getTransactionStatus() == NO_TRANSACTION) {
				throw new TransactionException("The JTA DataSource cannot be used outside a transaction");
			} else if (txContext.supportsXA()) {
				toClose = dataSource.getConnection();
				toReturn = new TxConnectionWrapper(toClose);
				txContext.registerXAResource(getXAResource(toClose));
			} else {
				throw new TransactionException(
						"There is a transaction active, but it does not support XA participants");
			}
		} catch (Exception sqle) {
			throw new TransactionException(
					"There was a problem getting hold of a database connection",
					sqle);
		}

		
		txContext.postCompletion(x -> {
				try {
					toClose.close();
				} catch (SQLException sqle) {
					// TODO log this
				}
			});
		
		txContext.putScopedValue(resourceId, toReturn);
		
		return toReturn;
	}

	
	private XAResource getXAResource(Connection conn) throws SQLException {
		if(conn instanceof XAConnectionWrapper) {
			return ((XAConnectionWrapper)conn).getXaResource();
		} else if(conn.isWrapperFor(XAConnectionWrapper.class)){
			return conn.unwrap(XAConnectionWrapper.class).getXaResource();
		} else {
			throw new IllegalArgumentException("The XAResource for the connection cannot be found");
		}
	}
}
