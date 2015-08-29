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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenericClause extends AbstractClause {
	private static final String REGEX = '(' + Grammar.PATH + "(?:;" + Grammar.PATH + ")*)(?:;\\s*(" + Grammar.PARAMETER + "))*";
	
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	private static final Pattern PATTERN_PARAMETER = Pattern.compile(Grammar.PARAMETER);
	
	public GenericClause(String clause) {
        super(clause);
	}
	
    @Override
    protected void processClauseString(String clauseString)
            throws IllegalArgumentException {
        Matcher matcher = PATTERN.matcher(clauseString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid clause: " + clauseString);
        }
        path = matcher.group(1);
        matcher = PATTERN_PARAMETER.matcher(clauseString);
        while(matcher.find()) {
            String group = matcher.group();
            if (group == null || group.length() == 0)
                continue;
            Parameter parameter = ParameterFactory.create(group);
            parameters.put(parameter.getName(), parameter);
        }
    }
}
