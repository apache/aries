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

import java.util.regex.Pattern;

public class SubsystemSymbolicNameHeader extends AbstractHeader {
	// TODO Add to constants.
	public static final String NAME = "Subsystem-SymbolicName";
	
	public SubsystemSymbolicNameHeader(String value) {
		super(NAME, value);
		if (getClauses().size() != 1)
			throw new IllegalArgumentException(/* TODO Message */);
		if (!Pattern.matches(Grammar.SYMBOLICNAME, getClauses().get(0).getPath()))
			throw new IllegalArgumentException(/* TODO Message */);
	}
	
	public String getSymbolicName() {
		return getClauses().get(0).getPath();
	}
}
