package org.apache.aries.tx.control.service.xa.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.transaction.xa.XAException;

import org.apache.aries.tx.control.service.common.activator.AbstractActivator;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.osgi.service.transaction.control.TransactionControl;

public class Activator extends AbstractActivator {

	private final GeronimoTransactionManager transactionManager;
	
	{
		try {
			transactionManager = new GeronimoTransactionManager();
		} catch (XAException e) {
			throw new RuntimeException("Unable to create the Transaction Manager");
		}
	}
	
	@Override
	protected TransactionControl getTransactionControl() {
		return new TransactionControlImpl(transactionManager);
	}
	
	@Override
	protected Dictionary<String, Object> getProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("osgi.local.enabled", Boolean.TRUE);
		props.put("osgi.xa.enabled", Boolean.TRUE);
		return props;
	}
}
