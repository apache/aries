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

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.service.cdi.annotations.Reference;
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
		_target = target;
	}

	@Override
	public ReferenceScope scope() {
		return ReferenceScope.BUNDLE;
	}

	@Override
	public String target() {
		return _target;
	}

	@Override
	public Class<?> service() {
		return Object.class;
	}

	private final String _target;

}
