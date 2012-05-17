package org.apache.aries.subsystem.core.archive;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.apache.aries.subsystem.core.internal.SubsystemResource;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemArchive implements Repository {
	private static final Logger logger = LoggerFactory.getLogger(SubsystemArchive.class);
	
	private final File directory;
	private final Collection<Resource> resources;
	
	private DeploymentManifest deploymentManifest;
	private SubsystemManifest subsystemManifest;
	
	public SubsystemArchive(SubsystemResource resource, File directory) throws IOException {
		this.directory = directory;
		resources = resource.getResources();
		setSubsystemManifest(resource.getSubsystemManifest());
		setDeploymentManifest(resource.getDeploymentManifest());
	}
	
	public SubsystemArchive(File content) throws Exception {
		logger.debug(LOG_ENTRY, "init", content);
		if (!content.isDirectory())
			throw new IllegalArgumentException("Not a directory: " + content.getAbsolutePath());
		directory = content;
		resources = new HashSet<Resource>();
		for (File file : content.listFiles()) {
			if (file.isDirectory() && "OSGI-INF".equals(file.getName())) {
				for (File f : file.listFiles()) {
					if (f.getName().equals("SUBSYSTEM.MF")) {
						subsystemManifest = new SubsystemManifest(f);
					}
					else if (f.getName().equals("DEPLOYMENT.MF")) {
						deploymentManifest = new DeploymentManifest(f);
					}
				}
			}
		}
		logger.debug(LOG_EXIT, "init");
	}
	
	public synchronized Collection<Capability> findProviders(Requirement requirement) {
		logger.debug(LOG_ENTRY, "findProviders", requirement);
		Collection<Capability> capabilities = new ArrayList<Capability>(1);
		for (Resource resource : resources) {
			logger.debug("Evaluating resource: " + resource);
			for (Capability capability : resource.getCapabilities(requirement.getNamespace())) {
				logger.debug("Evaluating capability: " + capability);
				if (ResourceHelper.matches(requirement, capability)) {
					logger.debug("Adding capability: " + capability);
					capabilities.add(capability);
				}
			}
		}
		logger.debug(LOG_EXIT, "findProviders", capabilities);
		return capabilities;
	}
	
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>(requirements.size());
		for (Requirement requirement : requirements)
			result.put(requirement, findProviders(requirement));
		return result;
	}
	
	public synchronized DeploymentManifest getDeploymentManifest() {
		return deploymentManifest;
	}
	
	public Collection<Resource> getResources() {
		return resources;
	}
	
	public synchronized SubsystemManifest getSubsystemManifest() {
		return subsystemManifest;
	}
	
	public synchronized void setDeploymentManifest(DeploymentManifest manifest) throws IOException {
		logger.debug(LOG_ENTRY, "setDeploymentManifest", manifest);
		File file = new File(directory, "OSGI-INF");
		if (!file.exists() && !file.mkdirs())
			throw new IOException("Unable to make directory for " + file.getCanonicalPath());
		file = new File(file, "DEPLOYMENT.MF");
		OutputStream out = new FileOutputStream(file);
		try {
			manifest.write(out);
		} finally {
			out.close();
		}
		
		deploymentManifest = manifest;
		logger.debug(LOG_EXIT, "setDeploymentManifest");
	}
	
	public synchronized void setSubsystemManifest(SubsystemManifest manifest) throws IOException {
		logger.debug(LOG_ENTRY, "setSubsystemManifest", manifest);
		File file = new File(directory, "OSGI-INF");
		if (!file.exists() && !file.mkdirs())
			throw new IOException("Unable to make directory for " + file.getCanonicalPath());
		file = new File(file, "SUBSYSTEM.MF");
		OutputStream out = new FileOutputStream(file);
		try {
			manifest.write(out);
		}
		finally {
			out.close();
		}
		// TODO What if the provided deployment manifest's file is out of sync?
		subsystemManifest = manifest;
		logger.debug(LOG_EXIT, "setSubsystemManifest");
	}
}
