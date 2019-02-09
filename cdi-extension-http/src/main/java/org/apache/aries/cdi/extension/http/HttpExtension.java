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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardContextSelect;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterAsyncSupported;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterDispatcher;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterName;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterPattern;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterServlet;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardListener;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletAsyncSupported;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletMultipart;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletName;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletPattern;
import org.apache.aries.cdi.extra.propertytypes.ServiceDescription;
import org.apache.aries.cdi.extra.propertytypes.ServiceRanking;
import org.jboss.weld.module.web.servlet.WeldInitialListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.annotations.Service;

public class HttpExtension implements Extension {

	public HttpExtension(Bundle bundle) {
		_bundle = bundle;
	}

	<X> void processWebFilter(@Observes @WithAnnotations(WebFilter.class) ProcessAnnotatedType<X> pat) {
		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		WebFilter webFilter = annotatedType.getAnnotation(WebFilter.class);

		final Set<Annotation> annotations = new HashSet<>(annotatedType.getAnnotations());

		if (!annotatedType.isAnnotationPresent(Service.class)) {
			annotations.add(Service.Literal.of(new Class[] {Filter.class}));
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			annotations.add(HttpWhiteboardContextSelect.Literal.of(getSelectedContext()));
		}

		if (!webFilter.description().isEmpty()) {
			annotations.add(ServiceDescription.Literal.of(webFilter.description()));
		}

		if (!webFilter.filterName().isEmpty()) {
			annotations.add(HttpWhiteboardFilterName.Literal.of(webFilter.filterName()));
		}

		if (webFilter.servletNames().length > 0) {
			annotations.add(HttpWhiteboardFilterServlet.Literal.of(webFilter.servletNames()));
		}

		if (webFilter.value().length > 0) {
			annotations.add(HttpWhiteboardFilterPattern.Literal.of(webFilter.value()));
		}
		else if (webFilter.urlPatterns().length > 0) {
			annotations.add(HttpWhiteboardFilterPattern.Literal.of(webFilter.urlPatterns()));
		}

		if (webFilter.dispatcherTypes().length > 0) {
			annotations.add(HttpWhiteboardFilterDispatcher.Literal.of(webFilter.dispatcherTypes()));
		}

		annotations.add(HttpWhiteboardFilterAsyncSupported.Literal.of(webFilter.asyncSupported()));

		pat.setAnnotatedType(new WebAnnotated<>(annotatedType, annotations));
	}

	<X> void processWebListener(@Observes @WithAnnotations(WebListener.class) ProcessAnnotatedType<X> pat) {
		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		WebListener webListener = annotatedType.getAnnotation(WebListener.class);

		final Set<Annotation> annotations = new HashSet<>(annotatedType.getAnnotations());

		if (!annotatedType.isAnnotationPresent(Service.class)) {
			List<Class<?>> listenerTypes = new ArrayList<>();

			Class<X> javaClass = annotatedType.getJavaClass();

			if (javax.servlet.ServletContextListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.ServletContextListener.class);
			}
			if (javax.servlet.ServletContextAttributeListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.ServletContextAttributeListener.class);
			}
			if (javax.servlet.ServletRequestListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.ServletRequestListener.class);
			}
			if (javax.servlet.ServletRequestAttributeListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.ServletRequestAttributeListener.class);
			}
			if (javax.servlet.http.HttpSessionListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.http.HttpSessionListener.class);
			}
			if (javax.servlet.http.HttpSessionAttributeListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.http.HttpSessionAttributeListener.class);
			}
			if (javax.servlet.http.HttpSessionIdListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.http.HttpSessionIdListener.class);
			}

			annotations.add(Service.Literal.of(listenerTypes.toArray(new Class<?>[0])));
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			annotations.add(HttpWhiteboardContextSelect.Literal.of(getSelectedContext()));
		}

		annotations.add(HttpWhiteboardListener.Literal.INSTANCE);

		if (!webListener.value().isEmpty()) {
			annotations.add(ServiceDescription.Literal.of(webListener.value()));
		}

		pat.setAnnotatedType(new WebAnnotated<>(annotatedType, annotations));
	}

	<X> void processWebServlet(@Observes @WithAnnotations(WebServlet.class) ProcessAnnotatedType<X> pat) {
		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		WebServlet webServlet = annotatedType.getAnnotation(WebServlet.class);

		final Set<Annotation> annotations = new HashSet<>(annotatedType.getAnnotations());

		if (!annotatedType.isAnnotationPresent(Service.class)) {
			annotations.add(Service.Literal.of(new Class[] {Servlet.class}));
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			annotations.add(HttpWhiteboardContextSelect.Literal.of(getSelectedContext()));
		}

		if (!webServlet.name().isEmpty()) {
			annotations.add(HttpWhiteboardServletName.Literal.of(webServlet.name()));
		}

		if (webServlet.value().length > 0) {
			annotations.add(HttpWhiteboardServletPattern.Literal.of(webServlet.value()));
		}
		else if (webServlet.urlPatterns().length > 0) {
			annotations.add(HttpWhiteboardServletPattern.Literal.of(webServlet.urlPatterns()));
		}

		annotations.add(ServiceRanking.Literal.of(webServlet.loadOnStartup()));

		// TODO Howto: INIT PARAMS ???

		annotations.add(HttpWhiteboardServletAsyncSupported.Literal.of(webServlet.asyncSupported()));

		if (!webServlet.description().isEmpty()) {
			annotations.add(ServiceDescription.Literal.of(webServlet.description()));
		}

		MultipartConfig multipartConfig = annotatedType.getAnnotation(MultipartConfig.class);

		if (multipartConfig != null) {
			annotations.add(HttpWhiteboardServletMultipart.Literal.of(true, multipartConfig.fileSizeThreshold(), multipartConfig.location(), multipartConfig.maxFileSize(), multipartConfig.maxRequestSize()));
		}

		// TODO HowTo: ServletSecurity ???

		pat.setAnnotatedType(new WebAnnotated<>(annotatedType, annotations));
	}

	void afterDeploymentValidation(
		@Observes @Priority(LIBRARY_AFTER + 800)
		AfterDeploymentValidation adv, BeanManager beanManager) {

		beanManager.getEvent().fireAsync(new Ready());
	}

	void ready(@ObservesAsync Ready ready, BeanManager beanManager) {
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
			LISTENER_CLASSES, new ListenerWrapper<>(initialListener), properties);
	}

	void beforeShutdown(@Observes BeforeShutdown bs) {
		if (_listenerRegistration != null && !destroyed.get()) {
			_listenerRegistration.unregister();
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
	private volatile ServiceRegistration<?> _listenerRegistration;
	private final AtomicBoolean destroyed = new AtomicBoolean(false);

	public static class Ready {}

	private class ListenerWrapper<T extends HttpSessionListener & ServletContextListener & ServletRequestListener>
		implements HttpSessionListener, ServletContextListener, ServletRequestListener {

		private final T delegate;

		public ListenerWrapper(T delegate) {
			this.delegate = delegate;
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			try {
				delegate.contextDestroyed(sce);
			}
			finally {
				destroyed.set(true);
			}
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			delegate.contextInitialized(sce);
		}

		@Override
		public void requestDestroyed(ServletRequestEvent sre) {
			delegate.requestDestroyed(sre);
		}

		@Override
		public void requestInitialized(ServletRequestEvent sre) {
			delegate.requestInitialized(sre);
		}

		@Override
		public void sessionCreated(HttpSessionEvent se) {
			delegate.sessionCreated(se);
		}

		@Override
		public void sessionDestroyed(HttpSessionEvent se) {
			delegate.sessionDestroyed(se);
		}

	}

	private class WebAnnotated<X> implements AnnotatedType<X> {

		private final AnnotatedType<X> annotatedType;
		private final Set<Annotation> annotations;

		public WebAnnotated(AnnotatedType<X> annotatedType, Set<Annotation> annotations) {
			this.annotatedType = annotatedType;
			this.annotations = annotations;
		}

		@Override
		public Type getBaseType() {
			return annotatedType.getBaseType();
		}

		@Override
		public Set<Type> getTypeClosure() {
			return annotatedType.getTypeClosure();
		}

		@Override
		public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
			return annotations.stream().filter(
				ann -> annotationType.isAssignableFrom(ann.annotationType())
			).map(
				ann -> annotationType.cast(ann)
			).findFirst().orElse(null);
		}

		@Override
		public Set<Annotation> getAnnotations() {
			return annotations;
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
			return annotations.stream().anyMatch(
				ann -> annotationType.isAssignableFrom(ann.annotationType())
			);
		}

		@Override
		public Class<X> getJavaClass() {
			return annotatedType.getJavaClass();
		}

		@Override
		public Set<AnnotatedConstructor<X>> getConstructors() {
			return annotatedType.getConstructors();
		}

		@Override
		public Set<AnnotatedMethod<? super X>> getMethods() {
			return annotatedType.getMethods();
		}

		@Override
		public Set<AnnotatedField<? super X>> getFields() {
			return annotatedType.getFields();
		}
	}

}
