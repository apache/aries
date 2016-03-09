package org.apache.aries.tx.control.service.xa.impl;

import org.apache.aries.tx.control.service.common.impl.AbstractTransactionContextImpl;
import org.apache.aries.tx.control.service.common.impl.AbstractTransactionControlImpl;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;

public class TransactionControlImpl extends AbstractTransactionControlImpl {
	
	GeronimoTransactionManager transactionManager;

	public TransactionControlImpl(GeronimoTransactionManager tm, Coordinator c) {
		super(c);
		this.transactionManager = tm;
	}

	@Override
	protected AbstractTransactionContextImpl startTransaction(Coordination currentCoord) {
		return new TransactionContextImpl(transactionManager, currentCoord);
	}
	
}
