/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.extension.http;

import static javax.interceptor.Interceptor.Priority.LIBRARY_AFTER;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.service.cdi.CDIConstants.CDI_CAPABILITY_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import org.jboss.weld.module.web.el.WeldELContextListener;
import org.jboss.weld.module.web.servlet.WeldInitialListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class HttpExtension implements Extension {

	public HttpExtension(Bundle bundle) {
		_bundle = bundle;
	}

	// TODO process javax.servlet.annotations annotations

	void afterDeploymentValidation(
		@Observes @Priority(LIBRARY_AFTER + 800)
		AfterDeploymentValidation adv, BeanManager beanManager) {

		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(SERVICE_DESCRIPTION, "Aries CDI - HTTP Portable Extension");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");
		properties.put(HTTP_WHITEBOARD_CONTEXT_SELECT, getSelectedContext());
		properties.put(HTTP_WHITEBOARD_LISTENER, Boolean.TRUE.toString());
		properties.put(SERVICE_RANKING, Integer.MAX_VALUE - 100);

		AnnotatedType<WeldInitialListener> annotatedType = beanManager.createAnnotatedType(WeldInitialListener.class);
		InjectionTargetFactory<WeldInitialListener> injectionTargetFactory = beanManager.getInjectionTargetFactory(annotatedType);
		Bean<WeldInitialListener> bean = beanManager.createBean(beanManager.createBeanAttributes(annotatedType), WeldInitialListener.class, injectionTargetFactory);

		WeldInitialListener initialListener = bean.create(beanManager.createCreationalContext(bean));

		_listenerRegistration = _bundle.getBundleContext().registerService(
			LISTENER_CLASSES, initialListener, properties);

		properties.put(
			SERVICE_DESCRIPTION, "Aries CDI - ELResolver Servlet Context Listener");

		_elAdaptorRegistration = _bundle.getBundleContext().registerService(
			ServletContextListener.class,
			new ServletContextListener() {
				@Override
				public void contextInitialized(ServletContextEvent event) {
					ServletContext servletContext = event.getServletContext();

					try {
						FrameworkUtil.getBundle(getClass()).loadClass("javax.servlet.jsp.JspFactory");
					}
					catch (ClassNotFoundException e) {
						servletContext.log("No JSP API found. Skiping ELResolver wiring for: " + _bundle);

						return;
					}

					javax.servlet.jsp.JspApplicationContext jspApplicationContext = javax.servlet.jsp.JspFactory.getDefaultFactory().getJspApplicationContext(servletContext);

					// Register the ELResolver with JSP
					jspApplicationContext.addELResolver(beanManager.getELResolver());

					// Register ELContextListener with JSP
					try {
						jspApplicationContext.addELContextListener(new WeldELContextListener());
					}
					catch (Exception e) {
						servletContext.log("Failure registering ELContextListener", e);
					}
				}

				@Override
				public void contextDestroyed(ServletContextEvent event) {
					// Nothing to do here
				}
			},
			properties);
	}

	void beforeShutdown(@Observes BeforeShutdown bs) {
		if (_listenerRegistration != null) {
			_listenerRegistration.unregister();
		}
		if (_elAdaptorRegistration != null) {
			_elAdaptorRegistration.unregister();
		}
	}

	private Map<String, Object> getAttributes() {
		BundleWiring bundleWiring = _bundle.adapt(BundleWiring.class);

		List<BundleWire> wires = bundleWiring.getRequiredWires(EXTENDER_NAMESPACE);

		Map<String, Object> cdiAttributes = Collections.emptyMap();

		for (BundleWire wire : wires) {
			BundleCapability capability = wire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String extender = (String)attributes.get(EXTENDER_NAMESPACE);

			if (extender.equals(CDI_CAPABILITY_NAME)) {
				BundleRequirement requirement = wire.getRequirement();
				cdiAttributes = requirement.getAttributes();
				break;
			}
		}

		return cdiAttributes;
	}

	private String getSelectedContext() {
		if (_contextSelect != null) {
			return _contextSelect;
		}

		return _contextSelect = getSelectedContext0();
	}

	private String getSelectedContext0() {
		Map<String, Object> attributes = getAttributes();

		if (attributes.containsKey(HTTP_WHITEBOARD_CONTEXT_SELECT)) {
			return (String)attributes.get(HTTP_WHITEBOARD_CONTEXT_SELECT);
		}

		Dictionary<String,String> headers = _bundle.getHeaders();

		if (headers.get(WEB_CONTEXT_PATH) != null) {
			return CONTEXT_PATH_PREFIX + headers.get(WEB_CONTEXT_PATH) + ')';
		}

		return DEFAULT_CONTEXT_FILTER;
	}

	private static final String CONTEXT_PATH_PREFIX = "(osgi.http.whiteboard.context.path=";
	private static final String DEFAULT_CONTEXT_FILTER = "(osgi.http.whiteboard.context.name=default)";
	private static final String[] LISTENER_CLASSES = new String[] {
		ServletContextListener.class.getName(),
		ServletRequestListener.class.getName(),
		HttpSessionListener.class.getName()
	};
	private static final String WEB_CONTEXT_PATH = "Web-ContextPath";

	private final Bundle _bundle;
	private String _contextSelect;
	private ServiceRegistration<?> _listenerRegistration;
	private ServiceRegistration<?> _elAdaptorRegistration;

}
