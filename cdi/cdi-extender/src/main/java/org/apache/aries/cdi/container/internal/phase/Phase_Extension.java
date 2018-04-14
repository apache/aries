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

package org.apache.aries.cdi.container.internal.phase;

import org.apache.aries.cdi.container.internal.container.ContainerState;

public class Phase_Extension implements Phase {

	public Phase_Extension(ContainerState containerState) {
/*		_containerState = containerState;
		_bundleContext = _containerState.bundle().getBundleContext();

		BundleWiring bundleWiring = _containerState.bundle().adapt(BundleWiring.class);

		_extensionDependencies = findExtensionDependencies(bundleWiring);
		_extensions = new ConcurrentSkipListMap<>(Comparator.reverseOrder());

		_containerState.setExtensionDependencies(_extensionDependencies);
*/	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void open() {
		// TODO Auto-generated method stub

	}

/*	@Override
	public void close() {
		if (_extensionTracker != null) {
			_extensionTracker.close();

			_extensionTracker = null;
		}
		else {
			_nextPhase.close();

			_nextPhase = null;
		}
	}

	@Override
	public void open() {
		if (!_extensionDependencies.isEmpty()) {
			_containerState.fire(CdiEvent.Type.WAITING_FOR_EXTENSIONS);

			Filter filter = createExtensionFilter(_extensionDependencies);

			_extensionTracker = new ServiceTracker<>(_bundleContext, filter, new ExtensionPhaseCustomizer());

			_extensionTracker.open();
		}
		else {
			_nextPhase = new Phase_Configuration(_containerState, _extensions.values());

			_nextPhase.open();
		}
	}

	Filter createExtensionFilter(List<ExtensionDependency> extentionDependencies) {
		try {
			StringBuilder sb = new StringBuilder("(&(objectClass=" + Extension.class.getName() + ")");

			if (extentionDependencies.size() > 1) sb.append("(|");

			for (ExtensionDependency dependency : extentionDependencies) {
				sb.append(dependency.toString());
			}

			if (extentionDependencies.size() > 1) sb.append(")");

			sb.append(")");

			return FrameworkUtil.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new RuntimeException(ise);
		}
	}

	List<ExtensionDependency> findExtensionDependencies(BundleWiring bundleWiring) {
		List<ExtensionDependency> extensionDependencies = new CopyOnWriteArrayList<>();
		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(CdiConstants.CDI_EXTENSION_NAMESPACE);

		for (BundleWire wire : requiredWires) {
			Map<String, Object> attributes = wire.getCapability().getAttributes();

			String extension = (String)attributes.get(CdiConstants.CDI_EXTENSION_NAMESPACE);

			if (extension != null) {
				ExtensionDependency extensionDependency = new ExtensionDependency(
					_bundleContext, wire.getProvider().getBundle().getBundleId(), extension);

				extensionDependencies.add(extensionDependency);
			}
		}

		return extensionDependencies;
	}

	private static final Logger _log = LoggerFactory.getLogger(Phase_Extension.class);

	private final BundleContext _bundleContext;
	private final ContainerState _containerState;
	private final List<ExtensionDependency> _extensionDependencies;
	private final Map<ServiceReference<Extension>, Metadata<Extension>> _extensions;
	private Phase _nextPhase;

	private ServiceTracker<Extension, ExtensionDependency> _extensionTracker;

	private class ExtensionPhaseCustomizer implements ServiceTrackerCustomizer<Extension, ExtensionDependency> {

		@Override
		public ExtensionDependency addingService(ServiceReference<Extension> reference) {
			ExtensionDependency trackedDependency = null;

			for (ExtensionDependency extensionDependency : _extensionDependencies) {
				if (extensionDependency.matches(reference)) {
					_extensionDependencies.remove(extensionDependency);
					trackedDependency = extensionDependency;

					Extension extension = _bundleContext.getService(reference);

					_extensions.put(reference, new ExtensionMetadata(extension, reference.getBundle().toString()));

					break;
				}
			}

			if ((trackedDependency != null) && _extensionDependencies.isEmpty()) {
				_nextPhase = new Phase_Configuration(_containerState, _extensions.values());

				_nextPhase.open();
			}
			else if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Still waiting for extensions {}", _extensionDependencies);
			}

			return trackedDependency;
		}

		@Override
		public void modifiedService(ServiceReference<Extension> reference, ExtensionDependency extentionDependency) {
		}

		@Override
		public void removedService(ServiceReference<Extension> reference, ExtensionDependency extentionDependency) {
			_extensionDependencies.add(extentionDependency);

			_extensions.remove(reference);

			try {
				_bundleContext.ungetService(reference);
			}
			catch (IllegalStateException ise) {
				if (_log.isWarnEnabled()) {
					_log.warn("CDIe - UngetService resulted in error", ise);
				}
			}

			if (!_extensionDependencies.isEmpty()) {
				_nextPhase.close();

				_nextPhase = null;

				_containerState.fire(CdiEvent.Type.WAITING_FOR_EXTENSIONS);
			}
		}

	}
*/
}
