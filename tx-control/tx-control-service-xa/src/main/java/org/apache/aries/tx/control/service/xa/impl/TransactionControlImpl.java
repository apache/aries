package org.apache.aries.tx.control.service.xa.impl;

import org.apache.aries.tx.control.service.common.impl.AbstractTransactionContextImpl;
import org.apache.aries.tx.control.service.common.impl.AbstractTransactionControlImpl;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;

public class TransactionControlImpl extends AbstractTransactionControlImpl {
	
	GeronimoTransactionManager transactionManager;

	public TransactionControlImpl(GeronimoTransactionManager tm) {
		this.transactionManager = tm;
	}

	@Override
	protected AbstractTransactionContextImpl startTransaction(boolean readOnly) {
		return new TransactionContextImpl(transactionManager, readOnly);
	}
	
}
