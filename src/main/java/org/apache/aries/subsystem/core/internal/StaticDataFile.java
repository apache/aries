package org.apache.aries.subsystem.core.internal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.Version;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;

public class StaticDataFile implements Resource {
	public static final String IDENTITY_TYPE = "org.apache.aries.subsystem.data.static";
	
	private final long lastSubsystemId;
	
	public StaticDataFile(InputStream content) throws IOException {
		DataInputStream dis = new DataInputStream(content);
		try {
			lastSubsystemId = dis.readLong();
		}
		finally {
			try {
				dis.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public StaticDataFile(File file) throws IOException {
		this(new FileInputStream(file));
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		List<Capability> result = new ArrayList<Capability>(1);
		if (namespace == null || namespace.equals(ResourceConstants.IDENTITY_NAMESPACE)) {
			OsgiIdentityCapability capability = new OsgiIdentityCapability(
					this,
					IDENTITY_TYPE,
					Version.emptyVersion,
					IDENTITY_TYPE);
			result.add(capability);
		}
		return result;
	}
	
	public long getLastSubsystemId() {
		return lastSubsystemId;
	}
	
	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}
	
	public void write(File file) throws IOException {
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(file, false));
		try {
			dos.writeLong(lastSubsystemId);
		}
		finally {
			try {
				dos.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
