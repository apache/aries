package org.apache.aries.cdi.test.beans;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.BundleContextBeanQualifier;
import org.osgi.framework.BundleContext;

@ApplicationScoped
@BundleContextBeanQualifier
public class BundleContextBean implements BeanService<BundleContext> {

	@Override
	public String doSomething() {
		return toString();
	}

	@Override
	public BundleContext get() {
		return _bundleContext;
	}

	@Inject
	private BundleContext _bundleContext;

}