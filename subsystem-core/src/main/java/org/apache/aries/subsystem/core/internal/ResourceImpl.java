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
package org.apache.aries.subsystem.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.spi.Capability;
import org.apache.aries.subsystem.spi.Requirement;
import org.apache.aries.subsystem.spi.Resource;
import org.osgi.framework.Version;

public class ResourceImpl implements Resource {
    private final Map<String,Object> attributes;

    public ResourceImpl(String symbolicName, Version version, String type, String location, Map<String,Object> attributes) {
    	if (attributes == null) {
    		attributes = new HashMap<String, Object>();
    	}
    	attributes.put(Resource.SYMBOLIC_NAME_ATTRIBUTE, symbolicName);
    	attributes.put(Resource.VERSION_ATTRIBUTE, version);
    	if (type == null || type.length() == 0) {
    		type = SubsystemConstants.RESOURCE_NAMESPACE_BUNDLE;
    	}
    	attributes.put(Resource.NAMESPACE_ATTRIBUTE, type);
    	attributes.put(Resource.LOCATION_ATTRIBUTE, location);
        this.attributes = attributes;
    }

    public String getSymbolicName() {
        return (String)attributes.get(Resource.SYMBOLIC_NAME_ATTRIBUTE);
    }

    public Version getVersion() {
        return (Version)attributes.get(Resource.VERSION_ATTRIBUTE);
    }

    public String getType() {
        return (String)attributes.get(Resource.NAMESPACE_ATTRIBUTE);
    }

    public String getLocation() {
        return (String)attributes.get(Resource.LOCATION_ATTRIBUTE);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public InputStream open() throws IOException {
        return new URL(getLocation()).openStream();
    }

	public List<Capability> getCapabilities(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Requirement> getRequirements(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
