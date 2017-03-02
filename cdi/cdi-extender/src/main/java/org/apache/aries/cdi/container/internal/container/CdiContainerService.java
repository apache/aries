package org.apache.aries.cdi.container.internal.container;

import javax.enterprise.inject.spi.BeanManager;

import org.osgi.service.cdi.CdiContainer;

public class CdiContainerService implements CdiContainer {

	@Override
	public BeanManager getBeanManager() {
		return _beanManager;
	}

	public void setBeanManager(BeanManager beanManager) {
		_beanManager = beanManager;
	}

	private volatile BeanManager _beanManager;

}