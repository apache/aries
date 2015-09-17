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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;

public class DynamicImportPackageHeader extends AbstractClauseBasedHeader<DynamicImportPackageHeader.Clause> implements RequirementHeader<DynamicImportPackageHeader.Clause> {
    public static class Clause extends AbstractClause {
		public Clause(String clause) {
			super(
					parsePath(clause, Patterns.WILDCARD_NAMES, true),
					parseParameters(clause, true), 
            		generateDefaultParameters(
            				VersionRangeAttribute.DEFAULT_VERSION));
		}
		
		public Collection<String> getPackageNames() {
			return Arrays.asList(path.split(";"));
		}
		
		public VersionRangeAttribute getVersionRangeAttribute() {
			return (VersionRangeAttribute)parameters.get(Constants.VERSION_ATTRIBUTE);
		}
        
		public List<DynamicImportPackageRequirement> toRequirements(Resource resource) {
			Collection<String> pkgs = getPackageNames();
			List<DynamicImportPackageRequirement> result = new ArrayList<DynamicImportPackageRequirement>(pkgs.size());
			for (String pkg : pkgs) {
				result.add(new DynamicImportPackageRequirement(pkg, this, resource));
			}
			return result;
		}
	}
	
	public static final String ATTRIBUTE_BUNDLE_SYMBOLICNAME = PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE;
	public static final String ATTRIBUTE_BUNDLE_VERSION = PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
	public static final String ATTRIBUTE_VERSION = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
	public static final String NAME = Constants.DYNAMICIMPORT_PACKAGE;
	
	public DynamicImportPackageHeader(Collection<Clause> clauses) {
		super(clauses);
	}
	
	public DynamicImportPackageHeader(String value) {
		super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
	}
	
	@Override
    public String getName() {
		return Constants.IMPORT_PACKAGE;
	}
	
	@Override
	public String getValue() {
		return toString();
	}
	
	@Override
	public List<DynamicImportPackageRequirement> toRequirements(Resource resource) {
		Collection<Clause> clauses = getClauses();
		List<DynamicImportPackageRequirement> result = new ArrayList<DynamicImportPackageRequirement>(clauses.size());
		for (Clause clause : clauses) {
			result.addAll(clause.toRequirements(resource));
		}
		return result;
	}
}
