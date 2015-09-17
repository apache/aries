/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.subsystem.core.archive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.core.capabilityset.SimpleFilter;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class ImportPackageHeader extends AbstractClauseBasedHeader<ImportPackageHeader.Clause> implements RequirementHeader<ImportPackageHeader.Clause> {
    public static class Clause extends AbstractClause {
    	private static final Collection<Parameter> defaultParameters = 
    			generateDefaultParameters(VersionRangeAttribute.DEFAULT_VERSION, ResolutionDirective.MANDATORY);
    	
		public static Clause valueOf(Requirement requirement) {
			String namespace = requirement.getNamespace();
			if (!ImportPackageRequirement.NAMESPACE.equals(namespace)) {
				throw new IllegalArgumentException("Invalid namespace:" + namespace);
			}
			Map<String, String> directives = requirement.getDirectives();
			String filter = directives.get(ImportPackageRequirement.DIRECTIVE_FILTER);
			Map<String, Object> attributes = SimpleFilter.attributes(filter);
			Map<String, Parameter> parameters = new HashMap<String, Parameter>(directives.size() + attributes.size());
			for (Map.Entry<String, String> entry : directives.entrySet()) {
				String key = entry.getKey();
				if (ImportPackageRequirement.DIRECTIVE_FILTER.equals(key)) {
					continue;
				}
				parameters.put(key, DirectiveFactory.createDirective(key, entry.getValue()));
			}
			for (Map.Entry<String, Object> entry : attributes.entrySet()) {
				String key = entry.getKey();
				if (ImportPackageRequirement.NAMESPACE.equals(key)) {
					continue;
				}
				parameters.put(key, AttributeFactory.createAttribute(key, String.valueOf(entry.getValue())));
			}
			String path = String.valueOf(attributes.get(ImportPackageRequirement.NAMESPACE));
			return new Clause(path, parameters, defaultParameters);
		}
		
		public Clause(String path, Map<String, Parameter> parameters, Collection<Parameter> defaultParameters) {
			super(path, parameters, defaultParameters);
		}
		
		public Clause(String clause) {
            super(
            		parsePath(clause, Patterns.PACKAGE_NAMES, true),
            		parseParameters(clause, true), 
            		defaultParameters);
		}
		
		public Collection<String> getPackageNames() {
			return Arrays.asList(path.split(";"));
		}
		
		
		public VersionRangeAttribute getVersionRangeAttribute() {
			return (VersionRangeAttribute)getAttribute(Constants.VERSION_ATTRIBUTE);
		}
		
		public ImportPackageRequirement toRequirement(Resource resource) {
			return new ImportPackageRequirement(this, resource);
		}
	}
	
	public static final String ATTRIBUTE_BUNDLE_SYMBOLICNAME = PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE;
	public static final String ATTRIBUTE_BUNDLE_VERSION = PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
	public static final String ATTRIBUTE_VERSION = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
	public static final String NAME = Constants.IMPORT_PACKAGE;
	public static final String DIRECTIVE_RESOLUTION = PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE;
	public static final String RESOLUTION_MANDATORY = PackageNamespace.RESOLUTION_MANDATORY;
	public static final String RESOLUTION_OPTIONAL = PackageNamespace.RESOLUTION_OPTIONAL;
	
	public ImportPackageHeader(Collection<Clause> clauses) {
	    super(clauses);
	}
	
	public ImportPackageHeader(String value) {
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
	public List<ImportPackageRequirement> toRequirements(Resource resource) {
		Collection<Clause> clauses = getClauses();
		List<ImportPackageRequirement> result = new ArrayList<ImportPackageRequirement>(clauses.size());
		for (Clause clause : clauses) {
			result.add(clause.toRequirement(resource));
		}
		return result;
	}
}
