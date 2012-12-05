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
package org.apache.aries.subsystem.obr.internal;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiRequirementAdapter implements Requirement {
	private static final Logger logger = LoggerFactory.getLogger(OsgiRequirementAdapter.class);
	
	private final org.osgi.resource.Requirement requirement;
	
	public OsgiRequirementAdapter(org.osgi.resource.Requirement requirement) {
		if (requirement == null)
			throw new NullPointerException("Missing required parameter: requirement");
		this.requirement = requirement;
	}

	public String getComment() {
		return null;
	}

	public String getFilter() {
		return requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
	}

	public String getName() {
		return NamespaceTranslator.translate(requirement.getNamespace());
	}

	public boolean isExtend() {
		return false;
	}

	public boolean isMultiple() {
		return false;
	}

	public boolean isOptional() {
		String resolution = requirement.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
		return Constants.RESOLUTION_OPTIONAL.equals(resolution);
	}

	public boolean isSatisfied(Capability capability) {
		logger.debug(LOG_ENTRY, "isSatisfied", capability);
		boolean result = ResourceHelper.matches(requirement, new FelixCapabilityAdapter(capability, null));
		logger.debug(LOG_EXIT, "isSatisfied", result);
		return result;
	}

}
