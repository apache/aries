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
package org.apache.aries.subsystem.itests.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class TestRequirement implements Requirement {
	public static class Builder {
		private final Map<String, Object> attributes = new HashMap<String, Object>();
		private final Map<String, String> directives = new HashMap<String, String>();
		
		private String namespace;
		private Resource resource;
		
		public Builder attribute(String name, Object value) {
			attributes.put(name,  value);
			return this;
		}
		
		public TestRequirement build() {
			return new TestRequirement(namespace, attributes, directives, resource);
		}
		
		public Builder directive(String name, String value) {
			directives.put(name, value);
			return this;
		}
		
		public Builder namespace(String value) {
			namespace = value;
			return this;
		}
		
		public Builder resource(Resource value) {
			resource = value;
			return this;
		}
	}
	
	private final Map<String, Object> attributes;
	private final Map<String, String> directives;
	private final String namespace;
	private final Resource resource;
	
	public TestRequirement(
			String namespace,
			Map<String, Object> attributes,
			Map<String, String> directives,
			Resource resource) {
		this.namespace = namespace;
		this.attributes = new HashMap<String, Object>(attributes);
		this.directives = new HashMap<String, String>(directives);
		this.resource = resource;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@Override
	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public Resource getResource() {
		return resource;
	}
}
