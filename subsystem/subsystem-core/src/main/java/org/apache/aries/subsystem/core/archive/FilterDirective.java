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

import org.apache.aries.subsystem.core.capabilityset.SimpleFilter;
import org.osgi.framework.Constants;

public class FilterDirective extends AbstractDirective {
	public static final String NAME = Constants.FILTER_DIRECTIVE;
	
	private final SimpleFilter filter;
	
	public FilterDirective(String value) {
		super(NAME, value);
		filter = SimpleFilter.parse(value);
	}

	@Override
    public String toString() {
		return new StringBuilder()
		.append(getName())
		.append(":=\"")
		.append(getValue())
		.append('\"')
		.toString();
	}
	
	@Override
    public int hashCode() {
        return 31 * 17 + filter.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o == this) {
    		return true;
    	}
    	if (!(o instanceof FilterDirective)) {
    		return false;
    	}
    	FilterDirective that = (FilterDirective)o;
    	return that.filter.equals(this.filter);
    }
}
