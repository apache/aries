package org.apache.aries.cdi.extension.jndi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

public class JndiExtension implements Extension {

	public BeanManager getBeanManager() {
		return _beanManager;
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager beanManager) {
		_beanManager = beanManager;
	}

	private BeanManager _beanManager;

}
