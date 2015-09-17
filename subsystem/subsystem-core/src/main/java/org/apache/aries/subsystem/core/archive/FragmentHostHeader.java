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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.resource.Resource;

public class FragmentHostHeader extends AbstractClauseBasedHeader<FragmentHostHeader.Clause> implements RequirementHeader<FragmentHostHeader.Clause> {
	public static class Clause extends AbstractClause {
		public static final String ATTRIBUTE_BUNDLEVERSION = Constants.BUNDLE_VERSION_ATTRIBUTE;
		
		private static final Pattern PATTERN_SYMBOLICNAME = Pattern.compile('(' + Grammar.SYMBOLICNAME + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
		    Parameter parameter = parameters.get(ATTRIBUTE_BUNDLEVERSION);
            if (parameter == null) {
                parameters.put(ATTRIBUTE_BUNDLEVERSION, 
                        new BundleVersionAttribute(
                                new VersionRange(
                                        VersionRange.LEFT_CLOSED, 
                                        new Version("0"), 
                                        null, 
                                        VersionRange.RIGHT_OPEN)));
            }
		}
		
		public Clause(String clause) {
		    super(clause);
		}
		

		public String getSymbolicName() {
			return path;
		}
		
		public FragmentHostRequirement toRequirement(Resource resource) {
			return new FragmentHostRequirement(this, resource);
		}
		
        @Override
        protected void processClauseString(String clauseString)
                throws IllegalArgumentException {

            Matcher matcher = PATTERN_SYMBOLICNAME.matcher(clauseString);
            if (!matcher.find())
                throw new IllegalArgumentException("Missing symbolic-name: " + clauseString);
            path = matcher.group();
            matcher.usePattern(PATTERN_PARAMETER);
            while (matcher.find()) {
                Parameter parameter = ParameterFactory.create(matcher.group());
                parameters.put(parameter.getName(), parameter);
            }
            fillInDefaults(parameters);
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
		super(value);
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
	protected Collection<Clause> processHeader(String header) {
	    
	    Set<Clause> lclauses = new HashSet<Clause>();
	    for (String clause : new ClauseTokenizer(header).getClauses())
	        lclauses.add(new Clause(clause));
        if (lclauses.size() != 1) {
            throw new IllegalArgumentException("A " + NAME + " header must have one and only one clause");
        }
	    return lclauses;
	}
	
	@Override
	public List<FragmentHostRequirement> toRequirements(Resource resource) {
		List<FragmentHostRequirement> requirements = new ArrayList<FragmentHostRequirement>(clauses.size());
		for (Clause clause : clauses)
			requirements.add(clause.toRequirement(resource));
		return requirements;
	}

}
