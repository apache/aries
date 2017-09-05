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

package org.apache.aries.cdi.container.internal.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.literal.AnyLiteral;
import org.apache.aries.cdi.container.internal.literal.DefaultLiteral;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.osgi.framework.BundleContext;

public class BundleContextBean implements Bean<BundleContext> {

	public BundleContextBean(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	@Override
	public BundleContext create(CreationalContext<BundleContext> creationalContext) {
		return _bundleContext;
	}

	@Override
	public void destroy(BundleContext instance, CreationalContext<BundleContext> creationalContext) {
	}

	@Override
	public Class<?> getBeanClass() {
		return BundleContext.class;
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		return Collections.emptySet();
	}

	@Override
	public String getName() {
		return "bundleContext";
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return DEFAULT_QUALIFIERS;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return Dependent.class;
	}

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() {
		return Collections.emptySet();
	}

	@Override
	public Set<Type> getTypes() {
		return TYPES;
	}

	@Override
	public boolean isAlternative() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format("BundleContext[%s]", _bundleContext);
		}

		return _string;
	}

	private static final Set<Annotation> DEFAULT_QUALIFIERS = Sets.hashSet(
		DefaultLiteral.INSTANCE, AnyLiteral.INSTANCE);
	private static final Set<Type> TYPES = Sets.immutableHashSet(BundleContext.class, Object.class);

	private final BundleContext _bundleContext;
	private volatile String _string;

}