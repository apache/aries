package org.apache.aries.tx.control.service.local.impl;

import org.apache.aries.tx.control.service.common.impl.AbstractTransactionContextImpl;
import org.apache.aries.tx.control.service.common.impl.AbstractTransactionControlImpl;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;

public class TransactionControlImpl extends AbstractTransactionControlImpl {

	public TransactionControlImpl(Coordinator c) {
		super(c);
	}

	@Override
	protected AbstractTransactionContextImpl startTransaction(Coordination currentCoord, boolean readOnly) {
		return new TransactionContextImpl(currentCoord, readOnly);
	}
	
}
