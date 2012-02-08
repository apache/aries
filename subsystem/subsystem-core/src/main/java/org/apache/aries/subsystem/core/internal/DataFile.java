package org.apache.aries.subsystem.core.internal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.Version;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;
import org.osgi.service.subsystem.Subsystem;

public class DataFile implements Resource {
	public static final String IDENTITY_TYPE = "org.apache.aries.subsystem.data";
	
	private final String location;
	private final String regionName;
	private final Subsystem.State state;
	private final long subsystemId;
	
	public DataFile(String location, String regionName, Subsystem.State state, long subsystemId) {
		this.location = location;
		this.regionName = regionName;
		this.state = state;
		this.subsystemId = subsystemId;
	}
	
	public DataFile(File file) throws IOException {
		if (!file.isFile())
			throw new IllegalArgumentException(file.getCanonicalPath());
		DataInputStream dis = new DataInputStream(new FileInputStream(file));
		try {
			location = dis.readUTF();
			regionName = dis.readUTF();
			state = Subsystem.State.valueOf(dis.readUTF());
			subsystemId = dis.readLong();
		}
		finally {
			try {
				dis.close();
			}
			catch (IOException e) {}
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof DataFile))
			return false;
		DataFile that = (DataFile)o;
		return that.location.equals(location)
				&& that.regionName.equals(regionName)
				&& that.subsystemId == subsystemId
				&& that.state.equals(state);
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + location.hashCode();
		result = 31 * result + regionName.hashCode();
		result = 31 * result + (int)(subsystemId^(subsystemId>>>32));
		result = 31 * result + state.hashCode();
		return result;
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
	
	public String getLocation() {
		return location;
	}
	
	public String getRegionName() {
		return regionName;
	}
	
	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}
	
	public /*synchronized*/ Subsystem.State getState() {
		return state;
	}
	
	public long getSubsystemId() {
		return subsystemId;
	}
	
	public void write(OutputStream out) throws IOException {
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeUTF(location);
			dos.writeUTF(regionName);
			dos.writeUTF(state.toString());
			dos.writeLong(subsystemId);
		}
		finally {
			try {
				dos.close();
			}
			catch (IOException e) {}
		}
	}
}
