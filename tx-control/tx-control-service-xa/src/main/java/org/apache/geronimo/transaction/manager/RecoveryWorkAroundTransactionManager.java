package org.apache.geronimo.transaction.manager;

import javax.transaction.xa.XAException;

import org.apache.geronimo.transaction.log.HOWLLog;

public class RecoveryWorkAroundTransactionManager extends GeronimoTransactionManager {

	public RecoveryWorkAroundTransactionManager(int timeout, XidFactory xidFactory, 
			HOWLLog log) throws XAException {
		super(timeout, xidFactory, log);
	}

	public NamedXAResourceFactory getNamedResource(String name) {
		return super.getNamedXAResourceFactory(name);
	}
}
