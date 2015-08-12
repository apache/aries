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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.AbstractCapability;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.resource.Resource;

public class FragmentHostCapability extends AbstractCapability {
	public static final String ATTRIBUTE_BUNDLE_VERSION = HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
	public static final String NAMESPACE = HostNamespace.HOST_NAMESPACE;
	
	private static Map<String, Object> initializeAttributes(BundleSymbolicNameHeader bsn, BundleVersionHeader version) {
		if (version == null) {
			version = new BundleVersionHeader();
		}
		Clause clause = bsn.getClauses().get(0);
		Collection<Attribute> attributes = clause.getAttributes();
		Map<String, Object> result = new HashMap<String, Object>(attributes.size() + 2);
		result.put(NAMESPACE, clause.getPath());
		result.put(ATTRIBUTE_BUNDLE_VERSION, version.getValue());
		for (Attribute attribute : attributes) {
			result.put(attribute.getName(), attribute.getValue());
		}
		return Collections.unmodifiableMap(result);
	}
	
	private static Map<String, String> initializeDirectives(Collection<Directive> directives) {
		if (directives.isEmpty())
			return Collections.emptyMap();
		Map<String, String> result = new HashMap<String, String>(directives.size());
		for (Directive directive : directives) {
			result.put(directive.getName(), directive.getValue());
		}
		return Collections.unmodifiableMap(result);
	}
	
	private final Map<String, Object> attributes;
	private final Map<String, String> directives;
	private final Resource resource;
	
	public FragmentHostCapability(BundleSymbolicNameHeader bsn, BundleVersionHeader version, Resource resource) {
		if (resource == null)
			throw new NullPointerException("Missing required parameter: resource");
		this.resource = resource;
		attributes = initializeAttributes(bsn, version);
		directives = initializeDirectives(bsn.getClauses().get(0).getDirectives());
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Map<String, String> getDirectives() {
		return directives;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public Resource getResource() {
		return resource;
	}
}
