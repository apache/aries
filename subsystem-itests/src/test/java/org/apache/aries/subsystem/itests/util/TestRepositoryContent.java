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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.repository.RepositoryContent;

public class TestRepositoryContent extends TestResource implements RepositoryContent {
	public static class Builder {
		private final List<TestCapability.Builder> capabilities = new ArrayList<TestCapability.Builder>();
		private final List<TestRequirement.Builder> requirements = new ArrayList<TestRequirement.Builder>();
		
		private byte[] content;
		
		public TestRepositoryContent build() {
			return new TestRepositoryContent(capabilities, requirements, content);
		}
		
		public Builder capability(TestCapability.Builder value) {
			capabilities.add(value);
			return this;
		}
		
		public Builder content(byte[] value) {
			content = value;
			return this;
		}
		
		public Builder requirement(TestRequirement.Builder value) {
			requirements.add(value);
			return this;
		}
	}
	
	private final byte[] content;
	
	public TestRepositoryContent(
			List<TestCapability.Builder> capabilities, 
			List<TestRequirement.Builder> requirements,
			byte[] content) {
		super(capabilities, requirements);
		this.content = content;
	}

	@Override
	public InputStream getContent() {
		try {
			return new ByteArrayInputStream(content);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
