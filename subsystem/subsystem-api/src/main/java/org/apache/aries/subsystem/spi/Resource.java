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
package org.apache.aries.subsystem.spi;

import java.util.List;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;


/**
 * A resource is the representation of a uniquely identified and typed data.
 * 
 * A resources can be wired together via capabilities and requirements.
 * 
 * TODO decide on identity characteristics of a revision. Given in OSGi multiple
 * bundles can be installed with same bsn/version this cannot be used as a key.
 * 
 * What then is identity of a resource? Object identity? URI (needs getter
 * method?)
 * 
 * @ThreadSafe
 * @version $Id: 8de47b041af414a2c6f2a21b5b34e89604c97806 $
 */
public interface Resource {
	/**
	 * Name space for OSGi bundle resources
	 */
	final String BUNDLE_NAMESPACE = "osgi.bundle";

	/**
	 * Attribute of type {@link String} used to specify the resource location.
	 */
	final String LOCATION_ATTRIBUTE = "location";

	/**
	 * Attribute of type {@link String} used to specify the resource symbolic name.
	 */
	final String SYMBOLIC_NAME_ATTRIBUTE = "symbolic-name";
	/**
	 * Attribute of type {@link Version} used to specify the resource version.
	 */
	final String VERSION_ATTRIBUTE = Constants.VERSION_ATTRIBUTE;
	/**
	 * Attribute of type {@link String} used to specify the resource name space.
	 */
	final String NAMESPACE_ATTRIBUTE = "namespace";

	/**
	 * Returns the capabilities declared by this resource.
	 * 
	 * @param namespace
	 *            The name space of the declared capabilities to return or
	 *            {@code null} to return the declared capabilities from all name
	 *            spaces.
	 * @return A list containing a snapshot of the declared {@link Capability}s,
	 *         or an empty list if this resource declares no capabilities in the
	 *         specified name space.
	 */
	List<Capability> getCapabilities(String namespace);

	/**
	 * Returns the requirements declared by this bundle resource.
	 * 
	 * @param namespace
	 *            The name space of the declared requirements to return or
	 *            {@code null} to return the declared requirements from all name
	 *            spaces.
	 * @return A list containing a snapshot of the declared {@link Requirement}
	 *         s, or an empty list if this resource declares no
	 *         requirements in the specified name space.
	 */
	List<Requirement> getRequirements(String namespace);

	/**
	 * Gets the attributes associated to this resource.
	 * 
	 * @return The attributes associated with the resource.
	 */
	public Map<String, Object> getAttributes();

}
