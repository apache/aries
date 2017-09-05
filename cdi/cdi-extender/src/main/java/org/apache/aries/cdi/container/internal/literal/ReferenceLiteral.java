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

package org.apache.aries.cdi.container.internal.literal;

import java.util.Objects;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceCardinality;
import org.osgi.service.cdi.annotations.ReferencePolicy;
import org.osgi.service.cdi.annotations.ReferencePolicyOption;
import org.osgi.service.cdi.annotations.ReferenceScope;

public class ReferenceLiteral extends AnnotationLiteral<Reference> implements Reference {

	private static final long serialVersionUID = 1L;

	/**
	 * @param target a target filter
	 * @return a literal instance of {@link Reference}
	 */
	public static ReferenceLiteral from(String target) {
		return new ReferenceLiteral(target);
	}

	public ReferenceLiteral(String target) {
		this(
			_blank,
			Object.class,
			ReferenceCardinality.DEFAULT,
			ReferencePolicy.DEFAULT,
			ReferencePolicyOption.DEFAULT,
			ReferenceScope.DEFAULT,
			target);
	}

	public ReferenceLiteral(
		String name,
		Class<?> service,
		ReferenceCardinality cardinality,
		ReferencePolicy policy,
		ReferencePolicyOption option,
		ReferenceScope scope,
		String target) {

		Objects.requireNonNull(target);

		_name = name;
		_service = service;
		_cardinality = cardinality;
		_policy = policy;
		_option = option;
		_scope = scope;
		_target = target;
	}

	@Override
	public ReferenceCardinality cardinality() {
		return _cardinality;
	}

	@Override
	public String name() {
		return _name;
	}

	@Override
	public ReferencePolicy policy() {
		return _policy;
	}

	@Override
	public ReferencePolicyOption policyOption() {
		return _option;
	}

	@Override
	public ReferenceScope scope() {
		return _scope;
	}

	@Override
	public Class<?> service() {
		return _service;
	}

	@Override
	public String target() {
		return _target;
	}

	private final static String _blank = "";

	private final ReferenceCardinality _cardinality;
	private final String _name;
	private final ReferencePolicyOption _option;
	private final ReferencePolicy _policy;
	private final ReferenceScope _scope;
	private final Class<?> _service;
	private final String _target;

}