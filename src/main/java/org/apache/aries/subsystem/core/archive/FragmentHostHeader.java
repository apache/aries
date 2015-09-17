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
import java.util.Collection;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.resource.Resource;

public class FragmentHostHeader extends AbstractClauseBasedHeader<FragmentHostHeader.Clause> implements RequirementHeader<FragmentHostHeader.Clause> {
	public static class Clause extends AbstractClause {
		public static final String ATTRIBUTE_BUNDLEVERSION = Constants.BUNDLE_VERSION_ATTRIBUTE;
		
		public Clause(String clause) {
		    super(
            		parsePath(clause, Patterns.SYMBOLIC_NAME, false), 
            		parseParameters(clause, false), 
            		generateDefaultParameters(
            				new BundleVersionAttribute(
        		    				new VersionRange(
        		    						VersionRange.LEFT_CLOSED, 
        		    						new Version("0"), 
        		    						null, 
                                            VersionRange.RIGHT_OPEN))));
		}
		

		public String getSymbolicName() {
			return path;
		}
		
		public FragmentHostRequirement toRequirement(Resource resource) {
			return new FragmentHostRequirement(this, resource);
		}
	}
	
	public static final String NAME = Constants.FRAGMENT_HOST;
	
	public FragmentHostHeader(Collection<Clause> clauses) {
	    super(clauses);
        if (clauses.size() != 1) {
            throw new IllegalArgumentException("A " + NAME + " header must have one and only one clause");
        }
	}
	
	public FragmentHostHeader(String value) {
		super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
		if (clauses.size() != 1) {
            throw new IllegalArgumentException("A " + NAME + " header must have one and only one clause");
        }
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getValue() {
		return toString();
	}
	
	@Override
	public List<FragmentHostRequirement> toRequirements(Resource resource) {
		List<FragmentHostRequirement> requirements = new ArrayList<FragmentHostRequirement>(clauses.size());
		for (Clause clause : clauses)
			requirements.add(clause.toRequirement(resource));
		return requirements;
	}

}
