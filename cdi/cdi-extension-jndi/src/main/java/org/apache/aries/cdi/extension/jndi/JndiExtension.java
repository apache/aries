package org.apache.aries.cdi.extension.jndi;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.CdiExtenderConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.jndi.JNDIConstants;

@Component(property = {CdiExtenderConstants.CDI_EXTENSION + "=jndi"})
public class JndiExtension implements Extension {

	@Activate
	void activate(BundleContext bundleContext) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JNDIConstants.JNDI_URLSCHEME, "java");

		_jndiObjectFactory = new JndiObjectFactory();
		_objectFactoryRegistration = bundleContext.registerService(
			ObjectFactory.class, _jndiObjectFactory, properties);
	}

	@Deactivate
	void deactivate() {
		_objectFactoryRegistration.unregister();
		_objectFactoryRegistration = null;
		_jndiObjectFactory = null;
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager beanManager) {
		@SuppressWarnings("serial")
		Set<Bean<?>> beans = beanManager.getBeans(BundleContext.class, new AnnotationLiteral<Any>() {});
		Bean<?> bean = beanManager.resolve(beans);
		CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
		BundleContext bundleContext = (BundleContext)beanManager.getReference(bean, BundleContext.class, ctx);

		_jndiObjectFactory.put(bundleContext, beanManager);
	}

	private JndiObjectFactory _jndiObjectFactory;
	private ServiceRegistration<ObjectFactory> _objectFactoryRegistration;

}
