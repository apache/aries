package org.apache.aries.tx.control.service.local.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.tx.control.service.common.activator.AbstractActivator;
import org.osgi.service.transaction.control.TransactionControl;

public class Activator extends AbstractActivator {

	@Override
	protected TransactionControl getTransactionControl() {
		return new TransactionControlImpl();
	}
	
	@Override
	protected Dictionary<String, Object> getProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("osgi.local.enabled", Boolean.TRUE);
		return props;
	}
}
