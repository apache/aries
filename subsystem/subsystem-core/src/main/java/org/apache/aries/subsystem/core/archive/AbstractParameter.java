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
	private final String myName;
	private final Object myValue;
	
	public AbstractParameter(String name, Object value) {
		myName = name;
		myValue = value;
	}
	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
	        return true;
	    if (obj == null)
	        return false;
	    if (getClass() != obj.getClass())
	        return false;
	    AbstractParameter other = (AbstractParameter) obj;
	    if (myName == null) {
	        if (other.myName != null)
	            return false;
	    } else
	        if (!myName.equals(other.myName))
	            return false;
	    if (myValue == null) {
	        if (other.myValue != null)
	            return false;
	    } else
	        if (!myValue.equals(other.myValue))
	            return false;
	    return true;
	}
	
	@Override
    public String getName() {
		return myName;
	}
	
	@Override
    public Object getValue() {
		return myValue;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((myName == null) ? 0 : myName.hashCode());
        result = prime * result + ((myValue == null) ? 0 : myValue.hashCode());
        return result;
    }

}
