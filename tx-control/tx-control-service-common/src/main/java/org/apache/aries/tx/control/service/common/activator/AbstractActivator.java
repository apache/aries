package org.apache.aries.tx.control.service.common.activator;

import java.util.Dictionary;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractActivator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(AbstractActivator.class);
	
	@Override
	public void start(BundleContext context) throws Exception {
		Dictionary<String, Object> properties = getProperties();
		logger.info("Registering a new TransactionControl service with properties {}", properties);
		context.registerService(
				TransactionControl.class, getTransactionControl(), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception { }

	protected abstract TransactionControl getTransactionControl();

	protected abstract Dictionary<String, Object> getProperties();
}
