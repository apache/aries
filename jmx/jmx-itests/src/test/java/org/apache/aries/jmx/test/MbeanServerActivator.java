package org.apache.aries.jmx.test;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MbeanServerActivator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();
		context.registerService(MBeanServer.class, mBeanServer, null);
	}

	public void stop(BundleContext context) throws Exception {
	}

}
