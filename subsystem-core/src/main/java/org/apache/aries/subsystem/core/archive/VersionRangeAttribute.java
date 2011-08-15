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

import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class VersionRangeAttribute extends AbstractAttribute {
	public static class RangedVersion extends Version {
		private final boolean inclusive;
		
		public RangedVersion() {
			super(Version.emptyVersion.toString());
			inclusive = true;
		}
		
		public RangedVersion(String value) {
			this(value, true);
		}
		
		public RangedVersion(String value, boolean inclusive) {
			super(value);
			this.inclusive = inclusive;
		}
		
		public boolean isExclusive() {
			return !inclusive;
		}
		
		public boolean isInclusive() {
			return inclusive;
		}
	}
	
	public static class Range {
		private static final String INTERVAL = "([\\[\\(])(" + Grammar.FLOOR + "),(" + Grammar.CEILING + ")([\\[\\)])";
		private static final String REGEX = INTERVAL + "|(" + Grammar.ATLEAST + ')';
		private static final Pattern PATTERN = Pattern.compile(REGEX);
		
		private final RangedVersion ceiling;
		private final RangedVersion floor;
		
		public Range() {
			ceiling = null;
			floor = new RangedVersion();
		}
		
		public Range(String value) {
			Matcher matcher = PATTERN.matcher(value);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("Invalid " + Constants.VERSION_ATTRIBUTE + " attribute value: " + value);
			}
			String floorSymbol = matcher.group(1);
			String floorStr = matcher.group(2);
			String ceilingStr = matcher.group(3);
			String ceilingSymbol = matcher.group(4);
			String atLeastStr = matcher.group(5);
			if (atLeastStr != null) {
				floor = new RangedVersion(atLeastStr);
				ceiling = null;
			}
			else {
				floor = new RangedVersion(floorStr, floorSymbol.equals("("));
				if (ceilingStr != null) {
					ceiling = new RangedVersion(ceilingStr, ceilingSymbol.equals("("));
				}
				else {
					ceiling = null;
				}
			}
		}
		
		public RangedVersion getCeiling() {
			return ceiling;
		}
		
		public RangedVersion getFloor() {
			return floor;
		}
	}
	
	private static final String REGEX = '(' + Grammar.INTERVAL + ")|(" + Grammar.ATLEAST + ')';
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	private final Range range;
	
	public VersionRangeAttribute() {
		super(Constants.VERSION_ATTRIBUTE, Version.emptyVersion.toString());
		range = new Range();
	}
			
	public VersionRangeAttribute(String value) {
		super(Constants.VERSION_ATTRIBUTE, value);
		Matcher matcher = PATTERN.matcher(value);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid " + Constants.VERSION_ATTRIBUTE + " attribute value: " + value);
		}
		range = new Range(matcher.group(1));
	}
	
	public StringBuilder appendToFilter(StringBuilder builder) {
		Range range = getRange();
		builder.append(getName()).append(">=").append(range.getFloor());
		if (range.getCeiling() != null) {
			builder.append(")(!(").append(getName()).append(">=").append(range.getCeiling()).append(')');
		}
		return builder;
	}

	public Range getRange() {
		return range;
	}
}
