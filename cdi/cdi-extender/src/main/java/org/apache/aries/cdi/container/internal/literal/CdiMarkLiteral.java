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

public class CdiMarkLiteral extends AnnotationLiteral<CdiMark> implements CdiMark {

	private static final long serialVersionUID = 1L;

	public static CdiMark from(int i) {
		return new CdiMarkLiteral(i);
	}

	public CdiMarkLiteral(int i) {
		_value = i;
	}

	@Override
	public int value() {
		return _value;
	}

	private final int _value;

}
