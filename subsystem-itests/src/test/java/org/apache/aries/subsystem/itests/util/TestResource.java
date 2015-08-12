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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class TestResource implements Resource {
	public static class Builder {
		private final List<TestCapability.Builder> capabilities = new ArrayList<TestCapability.Builder>();
		private final List<TestRequirement.Builder> requirements = new ArrayList<TestRequirement.Builder>();
		
		public TestResource build() {
			return new TestResource(capabilities, requirements);
		}
		
		public Builder capability(TestCapability.Builder value) {
			capabilities.add(value);
			return this;
		}
		
		public Builder requirement(TestRequirement.Builder value) {
			requirements.add(value);
			return this;
		}
	}
	
	private final List<Capability> capabilities;
	private final List<Requirement> requirements;
	
	public TestResource(List<TestCapability.Builder> capabilities, List<TestRequirement.Builder> requirements) {
		this.capabilities = new ArrayList<Capability>(capabilities.size());
		for (TestCapability.Builder builder : capabilities)
			this.capabilities.add(builder.resource(this).build());
		this.requirements = new ArrayList<Requirement>(requirements.size());
		for (TestRequirement.Builder builder : requirements)
            this.requirements.add(builder.resource(this).build());
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(capabilities);
		ArrayList<Capability> result = new ArrayList<Capability>(capabilities.size());
		for (Capability capability : capabilities)
			if (namespace.equals(capability.getNamespace()))
				result.add(capability);
		result.trimToSize();
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(requirements);
		ArrayList<Requirement> result = new ArrayList<Requirement>(requirements.size());
		for (Requirement requirement : requirements)
			if (namespace.equals(requirement.getNamespace()))
				result.add(requirement);
		result.trimToSize();
		return Collections.unmodifiableList(result);
	}
}
