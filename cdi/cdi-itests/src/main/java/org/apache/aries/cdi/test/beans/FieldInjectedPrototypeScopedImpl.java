package org.apache.aries.cdi.test.beans;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.cdi.test.interfaces.FieldInjectedReference;
import org.apache.aries.cdi.test.interfaces.PrototypeScoped;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceScope;
import org.osgi.service.cdi.annotations.Service;

@Service(type = {FieldInjectedPrototypeScopedImpl.class, FieldInjectedReference.class})
@Singleton
public class FieldInjectedPrototypeScopedImpl implements FieldInjectedReference<PrototypeScoped> {

	@Inject
	@Reference(target = "(key=value)")
	private ServiceReference<PrototypeScoped> genericReference;

	@Inject
	@Reference(service = PrototypeScoped.class, target = "(key=value)")
	private Map<String, Object> properties;

	@Inject
	@Reference(
		scope = ReferenceScope.PROTOTYPE,
		service = PrototypeScoped.class,
		target = "(key=value)"
	)
	@SuppressWarnings("rawtypes")
	private ServiceReference rawReference;

	@Inject
	@Reference(target = "(key=value)")
	private PrototypeScoped service;

	@Override
	public ServiceReference<PrototypeScoped> getGenericReference() {
		return genericReference;
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public ServiceReference getRawReference() {
		return rawReference;
	}

	@Override
	public PrototypeScoped getService() {
		return service;
	}

	@PostConstruct
	private void postConstructed() {
		System.out.println("PostConstructed " + this);
	}

	@PreDestroy
	private void preDestroyed() {
		System.out.println("PreDestroyed " + this);
	}

}