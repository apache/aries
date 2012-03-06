package org.apache.aries.subsystem.itests.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.osgi.resource.Requirement;
import org.osgi.service.repository.RepositoryContent;

public class TestRepositoryContent extends TestResource implements RepositoryContent {
	public static class Builder {
		private final List<TestCapability.Builder> capabilities = new ArrayList<TestCapability.Builder>();
		private final List<Requirement> requirements = new ArrayList<Requirement>();
		
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
		
		public Builder requirement(Requirement value) {
			requirements.add(value);
			return this;
		}
	}
	
	private final byte[] content;
	
	public TestRepositoryContent(
			List<TestCapability.Builder> capabilities, 
			List<Requirement> requirements,
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
