package org.apache.aries.subsystem.core.resource.tmp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.osgi.resource.Resource;

public class SubsystemArchive {
	private static DeploymentManifest computeDeploymentManifest(IDirectory directory) {
		IFile file = directory.getFile("OSGI-INF/DEPLOYMENT.MF");
		if (file == null)
			return null;
		return new DeploymentManifest(file);
	}
	
	private static Collection<Resource> computeResources(IDirectory directory) {
		ArrayList<Resource> resources = new ArrayList<Resource>();
		for (IFile file : directory.listFiles()) {
			String name = file.getName();
			if (!file.isDirectory()) {
				if (name.endsWith(".jar")) {
					resources.add(new BundleResource(file));
				}
				else if (name.endsWith(".esa")) {
//					resources.add(new SubsystemResource(new SubsystemArchive(file)));
				}
			}
		}
		return resources;
	}
	
	private static SubsystemManifest computeSubsystemManifest(IDirectory directory) {
		IFile file = directory.getFile("OSGI-INF/SUBSYSTEM.MF");
		if (file == null)
			return null;
		return new SubsystemManifest(file);
	}
	
	private final DeploymentManifest deploymentManifest;
	private final String name;
	private final Collection<Resource> resources;
	private final SubsystemManifest subsystemManifest;
	
	public SubsystemArchive(File content) {
		IDirectory directory = FileSystem.getFSRoot(content);
		deploymentManifest = computeDeploymentManifest(directory);
		name = null;
		resources = computeResources(directory);
		subsystemManifest = computeSubsystemManifest(directory);
	}
	
	public SubsystemArchive(IFile content) {
		deploymentManifest = null;
		name = null;
		resources = null;
		subsystemManifest = null;
	}
	
	public SubsystemArchive(InputStream content) throws IOException {
		this(new ZipInputStream(content));
	}
	
	public SubsystemArchive(ZipInputStream content) throws IOException {
		this(null, content);
	}
	
	public SubsystemArchive(String name, ZipInputStream content) throws IOException {
		try {
			DeploymentManifest deploymentManifest = null;
			ArrayList<Resource> resources = new ArrayList<Resource>();
			SubsystemManifest subsystemManifest = null;
			for (ZipEntry entry = content.getNextEntry(); entry != null; entry = content.getNextEntry()) {
				String entryName = entry.getName();
				if (entryName.equals("OSGI-INF/SUBSYSTEM.MF")) {
					subsystemManifest = null;
				}
				else if (entryName.equals("OSGI-INF/DEPLOYMENT.MF")) {
					deploymentManifest = null;
				}
				else if (!entry.isDirectory()) {
					if (entryName.endsWith(".jar")) {
						resources.add(null);
					}
					else if (entryName.endsWith(".esa")) {
//						resources.add(new SubsystemResource(new SubsystemArchive(entryName, content)));
					}
				}
				content.closeEntry();
			}
			resources.trimToSize();
			this.deploymentManifest = deploymentManifest;
			this.name = name;
			if (resources.isEmpty())
				this.resources = Collections.emptyList();
			else
				this.resources = Collections.unmodifiableList(resources);
			this.subsystemManifest = subsystemManifest;
		}
		finally {
			try {
				content.close();
			}
			catch (IOException e) {}
		}
	}
	
	public DeploymentManifest getDeploymentManifest() {
		return deploymentManifest;
	}
	
	public String getName() {
		return name;
	}
	
	public Collection<Resource> getResources() {
		return resources;
	}
	
	public SubsystemManifest getSubsystemManifest() {
		return subsystemManifest;
	}
}
