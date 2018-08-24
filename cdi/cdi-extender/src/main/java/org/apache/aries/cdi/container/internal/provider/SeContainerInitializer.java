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

package org.apache.aries.cdi.container.internal.provider;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.Extension;

public class SeContainerInitializer extends javax.enterprise.inject.se.SeContainerInitializer {

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer addBeanClasses(Class<?>... classes) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer addPackages(Class<?>... packageClasses) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer addPackages(
		boolean scanRecursively, Class<?>... packageClasses) {

		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer addPackages(Package... packages) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer addPackages(boolean scanRecursively, Package... packages) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer addExtensions(Extension... extensions) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	@SuppressWarnings("unchecked")
	public javax.enterprise.inject.se.SeContainerInitializer addExtensions(Class<? extends Extension>... extensions) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer enableInterceptors(Class<?>... interceptorClasses) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer enableDecorators(Class<?>... decoratorClasses) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer selectAlternatives(Class<?>... alternativeClasses) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	@SuppressWarnings("unchecked")
	public javax.enterprise.inject.se.SeContainerInitializer selectAlternativeStereotypes(
		Class<? extends Annotation>... alternativeStereotypeClasses) {

		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer addProperty(String key, Object value) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer setProperties(Map<String, Object> properties) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer disableDiscovery() {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public javax.enterprise.inject.se.SeContainerInitializer setClassLoader(ClassLoader classLoader) {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

	@Override
	public SeContainer initialize() {
		throw new UnsupportedOperationException("This API is not supported in OSGi");
	}

}
