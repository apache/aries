package org.apache.aries.cdi.test.beans;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.cdi.test.interfaces.BundleScoped;
import org.apache.aries.cdi.test.interfaces.FieldInjectedReference;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceScope;
import org.osgi.service.cdi.annotations.Service;

@Service(type = {FieldInjectedBundleScopedImpl.class, FieldInjectedReference.class})
@Singleton
public class FieldInjectedBundleScopedImpl implements FieldInjectedReference<BundleScoped> {

	@Inject
	@Reference(target = "(key=value)")
	private ServiceReference<BundleScoped> genericReference;

	@Inject
	@Reference(service = BundleScoped.class, target = "(key=value)")
	private Map<String, Object> properties;

	@Inject
	@Reference(
		scope = ReferenceScope.BUNDLE,
		service = BundleScoped.class,
		target = "(key=value)"
	)
	@SuppressWarnings("rawtypes")
	private ServiceReference rawReference;

	@Inject
	@Reference
	private BundleScoped service;

	@Override
	public ServiceReference<BundleScoped> getGenericReference() {
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
	public BundleScoped getService() {
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