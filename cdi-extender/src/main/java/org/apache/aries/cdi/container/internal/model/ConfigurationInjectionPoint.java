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

package org.apache.aries.cdi.container.internal.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.literal.AnyLiteral;
import org.apache.aries.cdi.container.internal.literal.ConfigurationLiteral;
import org.apache.aries.cdi.container.internal.literal.DefaultLiteral;
import org.apache.aries.cdi.container.internal.util.Sets;

public class ConfigurationInjectionPoint implements InjectionPoint {

	public ConfigurationInjectionPoint(Class<?> beanClass, String[] pids) {
		_beanClass = beanClass;
		_qualifiers = Sets.hashSet(DefaultLiteral.INSTANCE, AnyLiteral.INSTANCE, ConfigurationLiteral.from(pids));
	}

	@Override
	public Type getType() {
		return _beanClass;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return _qualifiers;
	}

	@Override
	public Bean<?> getBean() {
		return null;
	}

	@Override
	public Member getMember() {
		return null;
	}

	@Override
	public Annotated getAnnotated() {
		return null;
	}

	@Override
	public boolean isDelegate() {
		return false;
	}

	@Override
	public boolean isTransient() {
		return false;
	}

	private final Class<?> _beanClass;
	private final Set<Annotation> _qualifiers;

}