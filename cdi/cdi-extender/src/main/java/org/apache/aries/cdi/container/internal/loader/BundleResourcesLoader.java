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

package org.apache.aries.cdi.container.internal.loader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoadingException;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class BundleResourcesLoader implements ProxyServices, ResourceLoader {

	public BundleResourcesLoader(Bundle bundle, Bundle extenderBundle) {
		BundleWiring extenderWiring = extenderBundle.adapt(BundleWiring.class);

		List<Bundle> bundles = new ArrayList<>();

		bundles.add(bundle);
		bundles.add(extenderBundle);

		List<BundleWire> requiredWires = extenderWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);

		for (BundleWire bundleWire : requiredWires) {
			BundleCapability capability = bundleWire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String packageName = (String)attributes.get(PackageNamespace.PACKAGE_NAMESPACE);
			if (!packageName.startsWith("org.jboss.weld.")) {
				continue;
			}

			Bundle wireBundle = bundleWire.getProvider().getBundle();
			if (!bundles.contains(wireBundle)) {
				bundles.add(wireBundle);
			}
		}

		_classLoader = new BundleClassLoader(bundles.toArray(new Bundle[0]));
	}


	@Override
	public void cleanup() {
	}

	@Override
	public Class<?> classForName(String className) {
		try {
			return _classLoader.loadClass(className);
		}
		catch (ClassNotFoundException e) {
			throw new ResourceLoadingException(ERROR_LOADING_CLASS + className, e);
		}
		catch (LinkageError e) {
			throw new ResourceLoadingException(ERROR_LOADING_CLASS + className, e);
		}
		catch (TypeNotPresentException e) {
			throw new ResourceLoadingException(ERROR_LOADING_CLASS + className, e);
		}
	}

	@Override
	public ClassLoader getClassLoader(Class<?> proxiedBeanType) {
		return _classLoader;
	}

	@Override
	public Class<?> loadBeanClass(String className) {
		return classForName(className);
	}

	@Override
	public URL getResource(String name) {
		return _classLoader.getResource(name);
	}

	@Override
	public Collection<URL> getResources(String name) {
		try {
			return Collections.list(_classLoader.getResources(name));
		}
		catch (IOException e) {
			return Collections.emptyList();
		}
	}

	private static final String ERROR_LOADING_CLASS = "Error loading class ";

	private final ClassLoader _classLoader;

	public static Bundle[] getBundles(Bundle bundle, Bundle extenderBundle) {
		List<Bundle> bundles = new ArrayList<>();

		bundles.add(bundle);
		bundles.add(extenderBundle);

		BundleWiring extenderWiring = extenderBundle.adapt(BundleWiring.class);

		List<BundleWire> requiredWires = extenderWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);

		for (BundleWire bundleWire : requiredWires) {
			BundleCapability capability = bundleWire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String packageName = (String)attributes.get(PackageNamespace.PACKAGE_NAMESPACE);
			if (!packageName.startsWith("org.jboss.weld.")) {
				continue;
			}

			Bundle wireBundle = bundleWire.getProvider().getBundle();
			if (!bundles.contains(wireBundle)) {
				bundles.add(wireBundle);
			}
		}

		return bundles.toArray(new Bundle[0]);
	}
}
