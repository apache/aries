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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.core.archive.TypeAttribute;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceHelper {
	private static final Logger logger = LoggerFactory.getLogger(ResourceHelper.class);
	
	public static boolean areEqual(Resource resource1, Resource resource2) {
		if (getTypeAttribute(resource1).equals(getTypeAttribute(resource2))) {
			if (getSymbolicNameAttribute(resource1).equals(getSymbolicNameAttribute(resource2))) {
				if (getVersionAttribute(resource1).equals(getVersionAttribute(resource2))) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static String getContentAttribute(Resource resource) {
		// TODO Add to constants.
		return (String)getContentAttribute(resource, "osgi.content");
	}
	
	public static Object getContentAttribute(Resource resource, String name) {
		// TODO Add to constants.
		List<Capability> capabilities = resource.getCapabilities("osgi.content");
		Capability capability = capabilities.get(0);
		return capability.getAttributes().get(name);
	}
	
	public static Object getIdentityAttribute(Resource resource, String name) {
		List<Capability> capabilities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		Capability capability = capabilities.get(0);
		return capability.getAttributes().get(name);
	}
	
	public static String getLocation(Resource resource) {
		if (resource instanceof BundleResource)
			return ((BundleResource)resource).getLocation();
		if (resource instanceof BundleRevision)
			return ((BundleRevision)resource).getBundle().getLocation();
		if (resource instanceof BasicSubsystem)
			return ((BasicSubsystem)resource).getLocation();
		if (resource instanceof SubsystemResource)
			return ((SubsystemResource)resource).getLocation();
		if (resource instanceof RawSubsystemResource)
			return ((RawSubsystemResource)resource).getLocation().getValue();
		return getSymbolicNameAttribute(resource) + '@' + getVersionAttribute(resource);
	}
	
	public static Resource getResource(Requirement requirement, Repository repository) {
		Map<Requirement, Collection<Capability>> map = repository.findProviders(Arrays.asList(requirement));
		Collection<Capability> capabilities = map.get(requirement);
		return capabilities == null ? null : capabilities.size() == 0 ? null : capabilities.iterator().next().getResource();
	}
	
	public static String getSymbolicNameAttribute(Resource resource) {
		return (String)getIdentityAttribute(resource, IdentityNamespace.IDENTITY_NAMESPACE);
	}
	
	public static String getTypeAttribute(Resource resource) {
		String result = (String)getIdentityAttribute(resource, IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
		if (result == null)
			result = TypeAttribute.DEFAULT_VALUE;
		return result;
	}
	
	public static Version getVersionAttribute(Resource resource) {
		Version result = (Version)getIdentityAttribute(resource, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		if (result == null)
			result = Version.emptyVersion;
		return result;
	}
	
	public static boolean matches(Requirement requirement, Capability capability) {
		if (requirement == null && capability == null)
			return true;
		else if (requirement == null || capability == null) 
			return false;
		else if (!capability.getNamespace().equals(requirement.getNamespace())) 
			return false;
		else {
			String filterStr = requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
			if (filterStr != null) {
				try {
					if (!FrameworkUtil.createFilter(filterStr).matches(capability.getAttributes()))
						return false;
				}
				catch (InvalidSyntaxException e) {
					logger.debug("Requirement had invalid filter string: " + requirement, e);
					return false;
				}
			}
		}
		return matchMandatoryDirective(requirement, capability);
	}
	
	private static final String ATTR = "((?:\\s*[^=><~()]\\s*)+)";
	private static final String VALUE = "(?:\\\\\\\\|\\\\\\*|\\\\\\(|\\\\\\)|[^\\*()])+";
	private static final String FINAL = "(?:" + VALUE + ")?";
	private static final String STAR_VALUE = "(?:" + FINAL + "(?:\\*" + FINAL + ")*)";
	private static final String ANY = "(?:\\*" + STAR_VALUE + ")";
	private static final String INITIAL = FINAL;
	private static final String SUBSTRING = "(?:" + ATTR + "=" + INITIAL + ANY + FINAL + ")";
	private static final String PRESENT = "(?:" + ATTR + "=\\*)";
	private static final String LESS_EQ = "(?:<=)";
	private static final String GREATER_EQ = "(?:>=)";
	private static final String APPROX = "(?:~=)";
	private static final String EQUAL = "(?:=)";
	private static final String FILTER_TYPE = "(?:" + EQUAL + "|" + APPROX + "|" + GREATER_EQ + "|" + LESS_EQ + ")";
	private static final String SIMPLE = "(?:" + ATTR + FILTER_TYPE + VALUE + ")";
	private static final String OPERATION = "(?:" + SIMPLE + "|" + PRESENT + "|" + SUBSTRING + ")";
	
	private static final Pattern PATTERN = Pattern.compile(OPERATION);
	
	private static boolean matchMandatoryDirective(Requirement requirement, Capability capability) {
		if (!requirement.getNamespace().startsWith("osgi.wiring."))
			// Mandatory directives only affect osgi.wiring.* namespaces.
			return true;
		String mandatoryDirective = capability.getDirectives().get(AbstractWiringNamespace.CAPABILITY_MANDATORY_DIRECTIVE);
		if (mandatoryDirective == null)
			// There are no mandatory attributes to check.
			return true;
		String filterDirective = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filterDirective == null)
			// The filter specifies none of the mandatory attributes.
			return false;
		Set<String> attributeNames = new HashSet<String>();
		Matcher matcher = PATTERN.matcher(filterDirective);
		// Collect all of the attribute names from the filter.
		while (matcher.find())
			attributeNames.add(matcher.group(1));
		// Collect all of the mandatory attribute names.
		for (String s : mandatoryDirective.split(","))
			// Although all whitespace appears to be significant in a mandatory
			// directive value according to OSGi syntax (since it must be quoted 
			// due to commas), we'll anticipate issues here and trim off
			// whitespace around the commas.
			if (!attributeNames.contains(s.trim()))
				// The filter does not specify a mandatory attribute.
				return false;
		// The filter specifies all mandatory attributes.
		return true;
	}
}
