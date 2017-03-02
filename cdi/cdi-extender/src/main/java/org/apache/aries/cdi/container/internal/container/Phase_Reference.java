package org.apache.aries.cdi.container.internal.container;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.aries.cdi.container.internal.literal.ReferenceLiteral;
import org.apache.aries.cdi.container.internal.model.ReferenceInjectionPoint;
import org.apache.aries.cdi.container.internal.model.ReferenceModel;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.manager.BeanManagerImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Phase_Reference implements Phase {

	public Phase_Reference(
		List<ReferenceDependency> references, List<ServiceDeclaration> services, CdiContainerState cdiContainerState,
		Bootstrap bootstrap) {

		_references = references;
		_services = services;
		_cdiContainerState = cdiContainerState;
		_bootstrap = bootstrap;
		_bundle = _cdiContainerState.getBundle();
		_bundleContext = _bundle.getBundleContext();
	}

	@Override
	public void close() {
		if (_serviceTracker != null) {
			_serviceTracker.close();

			_serviceTracker = null;
		}
		else {
			_lock.lock();

			try {
				if (_nextPhase != null) {
					_nextPhase.close();

					_nextPhase = null;
				}
			}
			finally {
				_lock.unlock();
			}
		}
	}

	@Override
	public void open() {
		processDescriptorReferences((BeanManagerImpl)_cdiContainerState.getBeanManager());

		if (!_references.isEmpty()) {
			Filter filter = FilterBuilder.createReferenceFilter(_references);

			_cdiContainerState.fire(CdiEvent.Type.WAITING_FOR_SERVICES, filter.toString());

			_serviceTracker = new ServiceTracker<>(_bundleContext, filter, new ReferencePhaseCustomizer(_bootstrap));

			_serviceTracker.open();
		}

		_lock.lock();

		try {
			if ((_nextPhase == null) && dependenciesAreEmptyOrAllOptional()) {
				_nextPhase = new Phase_Publish(_references, _services, _cdiContainerState, _bootstrap);

				_nextPhase.open();
			}
		}
		finally {
			_lock.unlock();
		}
	}

	private boolean dependenciesAreEmptyOrAllOptional() {
		if (_references.isEmpty()) {
			return true;
		}

		for (ReferenceDependency referenceDependency : _references) {
			if (referenceDependency.getMinCardinality() > 0) {
				return false;
			}
		}

		return true;
	}

	private void processDescriptorReferences(BeanManagerImpl beanManagerImpl) {
		Collection<ReferenceModel> referenceModels = _cdiContainerState.getBeansModel().getReferenceModels();

		for (ReferenceModel referenceModel : referenceModels) {
			processReferenceModel(referenceModel, beanManagerImpl);
		}
	}

	private void processReferenceModel(ReferenceModel referenceModel, BeanManagerImpl beanManagerImpl) {
		try {
			Class<?> beanClass = _bundle.loadClass(referenceModel.getBeanClass());

			ReferenceDependency referenceDependency = new ReferenceDependency(
				beanManagerImpl, ReferenceLiteral.fromTarget(referenceModel.getTarget()),
				new ReferenceInjectionPoint(beanClass, referenceModel.getTarget()));

			_references.add(referenceDependency);
		}
		catch (ClassNotFoundException cnfe) {
			_log.error(
				"CDIe - osgi bean descriptor reference processing cannot load class {}",
				referenceModel.getBeanClass(), cnfe);
		}
		catch (InvalidSyntaxException ise) {
			_log.error("CDIe - osgi bean descriptor reference processing error", ise);
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(Phase_Reference.class);

	private final Bootstrap _bootstrap;
	private final Bundle _bundle;
	private final BundleContext _bundleContext;
	private final CdiContainerState _cdiContainerState;
	private final Lock _lock = new ReentrantLock(true);
	private Phase _nextPhase;
	private final List<ReferenceDependency> _references;
	private final List<ServiceDeclaration> _services;

	ServiceTracker<?, ?> _serviceTracker;

	private class ReferencePhaseCustomizer implements ServiceTrackerCustomizer<Object, Object> {

		public ReferencePhaseCustomizer(Bootstrap bootstrap) {
			_bootstrap = bootstrap;
		}

		@Override
		public Object addingService(ServiceReference<Object> reference) {
			_lock.lock();

			try {
				if (_nextPhase != null) {
					return null;
				}

				boolean matches = false;
				boolean resolved = true;

				for (ReferenceDependency referenceDependency : _references) {
					if (referenceDependency.matches(reference)) {
						referenceDependency.resolve(reference);
						matches = true;
					}
					if (!referenceDependency.isResolved()) {
						resolved = false;
					}
				}

				if (!matches) {
					return null;
				}

				if (resolved) {
					_nextPhase = new Phase_Publish(_references, _services, _cdiContainerState, _bootstrap);

					_nextPhase.open();
				}

				return new Object();
			}
			finally {
				_lock.unlock();
			}
		}

		@Override
		public void modifiedService(ServiceReference<Object> reference, Object object) {
		}

		@Override
		public void removedService(ServiceReference<Object> reference, Object object) {
			_lock.lock();

			try {
				if (_nextPhase != null) {
					_nextPhase.close();

					_nextPhase = null;

					_cdiContainerState.fire(CdiEvent.Type.WAITING_FOR_SERVICES);
				}

				for (ReferenceDependency referenceDependency : _references) {
					if (referenceDependency.matches(reference)) {
						referenceDependency.unresolve(reference);
					}
				}
			}
			finally {
				_lock.unlock();
			}
		}

		private final Bootstrap _bootstrap;

	}

}
