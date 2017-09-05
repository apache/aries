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

package org.apache.aries.cdi.container.internal.component;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.literal.CdiMarkLiteral;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.jboss.weld.injection.ForwardingInjectionPoint;

public class MarkedInjectionPoint extends ForwardingInjectionPoint {

	public MarkedInjectionPoint(InjectionPoint injectionPoint, Annotation annotation, int mark) {
		_delegate = injectionPoint;
		_mark = mark;
		_qualifiers = Sets.hashSet(injectionPoint.getQualifiers(), annotation, CdiMarkLiteral.from(_mark));
	}

	@Override
	protected InjectionPoint delegate() {
		return _delegate;
	}

	public InjectionPoint getDelegate() {
		return _delegate;
	}

	public int getMark() {
		return _mark;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return _qualifiers;
	}

	private final InjectionPoint _delegate;
	private final int _mark;
	private final Set<Annotation> _qualifiers;

}