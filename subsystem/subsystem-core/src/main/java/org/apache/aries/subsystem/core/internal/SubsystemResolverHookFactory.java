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

import java.util.Collection;

import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;

public class SubsystemResolverHookFactory implements ResolverHookFactory {
	private final Subsystems subsystems;
	public SubsystemResolverHookFactory(Subsystems subsystems) {
		if (subsystems == null)
			throw new NullPointerException("Missing required parameter: subsystems");
		this.subsystems = subsystems;
	}
	
	public ResolverHook begin(Collection<BundleRevision> triggers) {
		return new SubsystemResolverHook(subsystems);
	}
}
