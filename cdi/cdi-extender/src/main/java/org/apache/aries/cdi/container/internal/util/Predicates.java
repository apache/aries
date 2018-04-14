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

package org.apache.aries.cdi.container.internal.util;

import java.util.function.Predicate;

import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cm.ConfigurationEvent;

public class Predicates {

	private Predicates() {
		// no instances
	}

	public static Predicate<ConfigurationTemplateDTO> isMatchingConfiguration(ConfigurationEvent event) {
		return new MatchingConfigurationPredicate(event);
	}

	private static class MatchingConfigurationPredicate implements Predicate<ConfigurationTemplateDTO> {

		public MatchingConfigurationPredicate(ConfigurationEvent event) {
			this.event = event;
		}

		@Override
		public boolean test(ConfigurationTemplateDTO t) {
			if (t.pid == null) {
				return false;
			}
			if (((t.maximumCardinality == MaximumCardinality.MANY) && t.pid.equals(event.getFactoryPid())) ||
					((t.maximumCardinality == MaximumCardinality.ONE) && t.pid.equals(event.getPid()))) {
				return true;
			}
			return false;
		}

		private final ConfigurationEvent event;

	}
}
