package org.apache.aries.cdi.extension.jndi;

import java.util.Hashtable;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

public class JndiExtension implements Extension, ObjectFactory {

	@Override
	public Object getObjectInstance(
			Object obj, Name name, javax.naming.Context context, Hashtable<?, ?> environment)
		throws Exception {

		if (obj == null) {
			return _jndiContext;
		}

		return null;
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager beanManager) {
		_jndiContext = new JndiContext(beanManager);
	}

	private JndiContext _jndiContext;

}
