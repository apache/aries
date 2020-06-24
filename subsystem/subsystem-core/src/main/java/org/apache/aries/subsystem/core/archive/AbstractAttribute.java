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
package org.apache.aries.subsystem.core.archive;

public abstract class AbstractAttribute extends AbstractParameter implements Attribute {
	public AbstractAttribute(String name, Object value) {
//IC see: https://issues.apache.org/jira/browse/ARIES-728
		super(name, value);
	}
	
	@Override
    public StringBuilder appendToFilter(StringBuilder builder) {
//IC see: https://issues.apache.org/jira/browse/ARIES-737
		return builder.append('(').append(getName()).append('=').append(getValue()).append(')');
	}

	@Override
    public String toString() {
		return new StringBuilder()
				.append(getName())
				.append('=')
				.append(getValue())
				.toString();
	}
}
