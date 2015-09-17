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

public abstract class AbstractParameter implements Parameter {
	protected final String name;
	protected final Object value;
	
	public AbstractParameter(String name, Object value) {
		if (name == null || value == null) {
			throw new NullPointerException();
		}
		this.name = name;
		this.value = value;
	}
	
	@Override
    public String getName() {
		return name;
	}
	
	@Override
    public Object getValue() {
		return value;
	}

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + name.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
	public boolean equals(Object o) {
		if (o == this) {
    		return true;
    	}
    	if (!(o instanceof AbstractParameter)) {
    		return false;
    	}
    	AbstractParameter that = (AbstractParameter)o;
    	return that.name.equals(this.name)
    			&& that.value.equals(this.value);
	}
}
