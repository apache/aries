package org.apache.aries.tx.control.jpa.xa.impl;

import java.util.function.Function;

import javax.persistence.EntityManager;

import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;

public class DelayedJPAEntityManagerProvider implements JPAEntityManagerProvider {
	
	private final Function<TransactionControl, JPAEntityManagerProvider> wireToTransactionControl;
	
	private JPAEntityManagerProvider delegate;
	
	private TransactionControl wiredTxControl;

	public DelayedJPAEntityManagerProvider(Function<TransactionControl, JPAEntityManagerProvider> wireToTransactionControl) {
		this.wireToTransactionControl = wireToTransactionControl;
	}

	@Override
	public EntityManager getResource(TransactionControl txControl) throws TransactionException {
		synchronized (wireToTransactionControl) {
			
			if(wiredTxControl != null && !wiredTxControl.equals(txControl)) {
				throw new TransactionException("This JPAEntityManagerProvider has already been wired to a different Transaction Control service.");
			}
			if(delegate == null) {
				delegate = wireToTransactionControl.apply(txControl);
				wiredTxControl = txControl;
			}
		}
		return delegate.getResource(txControl);
	}
	
	

}
