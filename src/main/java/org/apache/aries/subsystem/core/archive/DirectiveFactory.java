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

public class DirectiveFactory {
	public static Directive createDirective(String name, String value) {
		if (ResolutionDirective.NAME.equals(name))
			return ResolutionDirective.getInstance(value);
		if (StartOrderDirective.NAME.equals(name))
			return new StartOrderDirective(value);
		if (FilterDirective.NAME.equals(name))
			return new FilterDirective(value);
		if (EffectiveDirective.NAME.equals(name))
			return EffectiveDirective.getInstance(value);
		if (VisibilityDirective.NAME.equals(name))
			return VisibilityDirective.getInstance(value);
		if (ProvisionPolicyDirective.NAME.equals(name))
			return ProvisionPolicyDirective.getInstance(value);
		if (ReferenceDirective.NAME.equals(name))
			return ReferenceDirective.getInstance(value);
		return new GenericDirective(name, value);
	}
}
