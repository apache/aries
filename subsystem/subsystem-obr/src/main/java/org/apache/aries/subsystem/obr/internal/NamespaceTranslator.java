/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.obr.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;

public class NamespaceTranslator {
	private static final Map<String, String> namespaces = computeNamespaces();
	
	private static Map<String, String> computeNamespaces() {
		Map<String, String> result = new HashMap<String, String>(8);
		result.put(PackageNamespace.PACKAGE_NAMESPACE, org.apache.felix.bundlerepository.Capability.PACKAGE);
		result.put(ServiceNamespace.SERVICE_NAMESPACE, org.apache.felix.bundlerepository.Capability.SERVICE);
		result.put(BundleNamespace.BUNDLE_NAMESPACE, org.apache.felix.bundlerepository.Capability.BUNDLE);
		result.put(org.apache.felix.bundlerepository.Capability.PACKAGE, PackageNamespace.PACKAGE_NAMESPACE);
		result.put(org.apache.felix.bundlerepository.Capability.SERVICE, ServiceNamespace.SERVICE_NAMESPACE);
		result.put(org.apache.felix.bundlerepository.Capability.BUNDLE, BundleNamespace.BUNDLE_NAMESPACE);
		return Collections.unmodifiableMap(result);
	}
	
	public static String translate(String namespace) {
		String result = namespaces.get(namespace);
		if (result == null)
			result = namespace;
		return result;
	}
}
