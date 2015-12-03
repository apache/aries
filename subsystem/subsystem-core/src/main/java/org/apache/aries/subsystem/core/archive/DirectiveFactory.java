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

import java.util.HashMap;
import java.util.Map;

public class DirectiveFactory {
	private interface Creator {
		Directive create(String value);
	}
	
	private static final Map<String, Creator> map = new HashMap<String, Creator>();
	
	static {
		map.put(AriesProvisionDependenciesDirective.NAME, new Creator() {
			@Override
			public Directive create(String value) {
				return AriesProvisionDependenciesDirective.getInstance(value);
			}
		});
		map.put(CardinalityDirective.NAME, new Creator() {
			@Override
			public Directive create(String value) {
				return CardinalityDirective.getInstance(value);
			}
		});
		map.put(EffectiveDirective.NAME, new Creator() {
			@Override
			public Directive create(String value) {
				return EffectiveDirective.getInstance(value);
			}
		});
		map.put(FilterDirective.NAME, new Creator() {
			@Override
			public Directive create(String value) {
				return new FilterDirective(value);
			}
		});
		map.put(ProvisionPolicyDirective.NAME, new Creator() {
			@Override
			public Directive create(String value) {
				return ProvisionPolicyDirective.getInstance(value);
			}
		});
		map.put(ReferenceDirective.NAME, new Creator() {
			@Override
			public Directive create(String value) {
				return ReferenceDirective.getInstance(value);
			}
		});
		map.put(ResolutionDirective.NAME, new Creator() {
			@Override
			public Directive create(String value) {
				return ResolutionDirective.getInstance(value);
			}
		});
		map.put(StartOrderDirective.NAME, new Creator() {
			@Override
			public Directive create(String value) {
				return new StartOrderDirective(value);
			}
		});
		map.put(VisibilityDirective.NAME, new Creator() {
			@Override
			public Directive create(String value) {
				return VisibilityDirective.getInstance(value);
			}
		});
	}
	
	public static Directive createDirective(String name, String value) {
		Creator creator = map.get(name);
		if (creator == null) {
			return new GenericDirective(name, value);
		}
		return creator.create(value);
	}
}
