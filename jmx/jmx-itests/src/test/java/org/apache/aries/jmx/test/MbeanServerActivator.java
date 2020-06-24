package org.apache.aries.jmx.test;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MbeanServerActivator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
//IC see: https://issues.apache.org/jira/browse/ARIES-1194
		MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();
		context.registerService(MBeanServer.class, mBeanServer, null);
	}

	public void stop(BundleContext context) throws Exception {
	}

}
