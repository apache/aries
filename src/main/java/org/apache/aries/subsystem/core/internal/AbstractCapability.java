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
package org.apache.aries.subsystem.core.internal;

import org.osgi.resource.Capability;

public abstract class AbstractCapability implements Capability {
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Capability))
			return false;
		Capability c = (Capability)o;
		return c.getNamespace().equals(getNamespace())
				&& c.getAttributes().equals(getAttributes())
				&& c.getDirectives().equals(getDirectives())
				&& c.getResource().equals(getResource());
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + getNamespace().hashCode();
		result = 31 * result + getAttributes().hashCode();
		result = 31 * result + getDirectives().hashCode();
		result = 31 * result + getResource().hashCode();
		return result;
	}
	
	@Override
	public String toString() {
		return new StringBuilder().append("[Capability: ")
				.append("namespace=").append(getNamespace())
				.append(", attributes=").append(getAttributes())
				.append(", directives=").append(getDirectives())
				.append(", resource=").append(getResource()).append(']')
				.toString();
	}
}
