package org.apache.aries.subsystem.core.archive;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.aries.subsystem.core.ResourceHelper;
import org.apache.aries.subsystem.core.internal.DataFile;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.apache.aries.subsystem.core.internal.StaticDataFile;
import org.apache.aries.subsystem.core.resource.AbstractRequirement;
import org.apache.aries.subsystem.core.resource.BundleResource;
import org.apache.aries.subsystem.core.resource.SubsystemFileResource;
import org.osgi.framework.Constants;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.subsystem.SubsystemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemArchive implements Repository, RepositoryContent, Resource {
	private static final Logger logger = LoggerFactory.getLogger(SubsystemArchive.class);
	
	private final File directory;
	private final Map<Resource, URL> resources = new HashMap<Resource, URL>();
	
	public SubsystemArchive(File content) throws Exception {
		logger.debug(LOG_ENTRY, "init", content);
		if (!content.exists())
			throw new IllegalArgumentException(content.getName());
		if (content.isDirectory()) {
			directory = content;
			processDirectory(content);
		}
		else { // It's a file.
			directory = content.getParentFile();
			processFile(content);
		}
		logger.debug(LOG_EXIT, "init");
	}
	
	@Override
	public synchronized Collection<Capability> findProviders(Requirement requirement) {
		logger.debug(LOG_ENTRY, "findProviders", requirement);
		Collection<Capability> capabilities = new ArrayList<Capability>(1);
		for (Resource resource : resources.keySet()) {
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
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null || namespace.equals(ResourceConstants.IDENTITY_NAMESPACE)) {
			List<Capability> result = new ArrayList<Capability>(1);
			OsgiIdentityCapability capability = new OsgiIdentityCapability(
					this,
					getSubsystemManifest().getSubsystemSymbolicName().getSymbolicName(),
					getSubsystemManifest().getSubsystemVersion().getVersion(),
					SubsystemConstants.IDENTITY_TYPE_SUBSYSTEM);
			result.add(capability);
			return result;
		}
		return Collections.emptyList();
	}
	
	@Override
	public synchronized InputStream getContent() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(baos);
		for (Resource resource : getResources()) {
			ZipEntry ze = new ZipEntry(
					new StringBuilder(ResourceHelper.getSymbolicNameAttribute(resource))
					.append(';')
					.append(ResourceHelper.getVersionAttribute(resource))
					.append(';')
					.append(ResourceHelper.getTypeAttribute(resource))
					.toString());
			try {
				zos.putNextEntry(ze);
				InputStream is = ((RepositoryContent) resource).getContent();
				byte[] bytes = new byte[2048];
				int read;
				while ((read = is.read(bytes)) != -1)
					zos.write(bytes, 0, read);
				zos.closeEntry();
			} 
			finally {
				try {
					zos.close();
				} 
				catch (IOException e) {
					logger.debug("Unable to close the stream: ", e);
				}
			}
		}
		return new ByteArrayInputStream(baos.toByteArray());
	}
	
	public synchronized DataFile getDataFile() {
		Collection<Capability> capabilities = findProviders(new AbstractRequirement() {
			@Override
			public Map<String, Object> getAttributes() {
				return Collections.emptyMap();
			}

			@Override
			public Map<String, String> getDirectives() {
				Map<String, String> directives = new HashMap<String, String>(1);
				directives.put(
						Constants.FILTER_DIRECTIVE, 
						new StringBuilder()
								.append('(')
								.append(ResourceConstants.IDENTITY_TYPE_ATTRIBUTE)
								.append('=')
								.append(DataFile.IDENTITY_TYPE)
								.append(')')
								.toString());
				return Collections.unmodifiableMap(directives);
			}

			@Override
			public String getNamespace() {
				return ResourceConstants.IDENTITY_NAMESPACE;
			}

			@Override
			public Resource getResource() {
				return null;
			}
		});
		if (capabilities.isEmpty())
			return null;
		return (DataFile)capabilities.iterator().next().getResource();
	}
	
	public synchronized DeploymentManifest getDeploymentManifest() {
		Collection<Capability> capabilities = findProviders(new AbstractRequirement() {
			@Override
			public Map<String, Object> getAttributes() {
				return Collections.emptyMap();
			}

			@Override
			public Map<String, String> getDirectives() {
				Map<String, String> directives = new HashMap<String, String>(1);
				directives.put(
						Constants.FILTER_DIRECTIVE, 
						new StringBuilder()
								.append('(')
								.append(ResourceConstants.IDENTITY_TYPE_ATTRIBUTE)
								.append('=')
								.append(DeploymentManifest.IDENTITY_TYPE)
								.append(')')
								.toString());
				return Collections.unmodifiableMap(directives);
			}

			@Override
			public String getNamespace() {
				return ResourceConstants.IDENTITY_NAMESPACE;
			}

			@Override
			public Resource getResource() {
				return null;
			}
		});
		if (capabilities.isEmpty())
			return null;
		return (DeploymentManifest)capabilities.iterator().next().getResource();
	}
	
	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}
	
	public synchronized Collection<Resource> getResources() {
		Collection<Capability> capabilities = findProviders(new AbstractRequirement() {
			@Override
			public Map<String, Object> getAttributes() {
				return Collections.emptyMap();
			}

			@Override
			public Map<String, String> getDirectives() {
				Map<String, String> directives = new HashMap<String, String>(1);
				directives.put(
						Constants.FILTER_DIRECTIVE, 
						new StringBuilder("(")
								.append(ResourceConstants.IDENTITY_TYPE_ATTRIBUTE)
								.append("=osgi.*)")
								.toString());
				return Collections.unmodifiableMap(directives);
			}

			@Override
			public String getNamespace() {
				return ResourceConstants.IDENTITY_NAMESPACE;
			}

			@Override
			public Resource getResource() {
				return null;
			}
		});
		if (capabilities.isEmpty())
			return Collections.emptyList();
		Collection<Resource> resources = new HashSet<Resource>(capabilities.size());
		for (Capability capability : capabilities) {
			resources.add(capability.getResource());
		}
		return resources;
	}
	
	public synchronized StaticDataFile getStaticDataFile() {
		Collection<Capability> capabilities = findProviders(new AbstractRequirement() {
			@Override
			public Map<String, Object> getAttributes() {
				return Collections.emptyMap();
			}

			@Override
			public Map<String, String> getDirectives() {
				Map<String, String> directives = new HashMap<String, String>(1);
				directives.put(
						Constants.FILTER_DIRECTIVE, 
						new StringBuilder()
								.append('(')
								.append(ResourceConstants.IDENTITY_TYPE_ATTRIBUTE)
								.append('=')
								.append(StaticDataFile.IDENTITY_TYPE)
								.append(')')
								.toString());
				return Collections.unmodifiableMap(directives);
			}

			@Override
			public String getNamespace() {
				return ResourceConstants.IDENTITY_NAMESPACE;
			}

			@Override
			public Resource getResource() {
				return null;
			}
		});
		if (capabilities.isEmpty())
			return null;
		return (StaticDataFile)capabilities.iterator().next().getResource();
	}
	
	public synchronized SubsystemManifest getSubsystemManifest() {
		logger.debug(LOG_ENTRY, "getSubsystemManifest");
		Collection<Capability> capabilities = findProviders(new AbstractRequirement() {
			@Override
			public Map<String, Object> getAttributes() {
				return Collections.emptyMap();
			}

			@Override
			public Map<String, String> getDirectives() {
				Map<String, String> directives = new HashMap<String, String>(1);
				directives.put(
						Constants.FILTER_DIRECTIVE, 
						new StringBuilder()
								.append('(')
								.append(ResourceConstants.IDENTITY_TYPE_ATTRIBUTE)
								.append('=')
								.append(SubsystemManifest.IDENTITY_TYPE)
								.append(')')
								.toString());
				return Collections.unmodifiableMap(directives);
			}

			@Override
			public String getNamespace() {
				return ResourceConstants.IDENTITY_NAMESPACE;
			}

			@Override
			public Resource getResource() {
				return null;
			}
		});
		SubsystemManifest result = null;
		if (!capabilities.isEmpty())
			result = (SubsystemManifest)capabilities.iterator().next().getResource();
		logger.debug(LOG_EXIT, "getSubsystemManifest", result);
		return result;
	}
	
	public synchronized void setDataFile(DataFile dataFile) throws IOException {
		DataFile old = getDataFile();
		// TODO Add to constants.
		File file = new File(directory, DataFile.IDENTITY_TYPE);
		OutputStream out = new FileOutputStream(file);
		try {
			dataFile.write(out);
			if (old != null)
				resources.remove(old);
			resources.put(dataFile, file.toURI().toURL());
		} finally {
			out.close();
		}
	}
	
	public synchronized void setDeploymentManifest(DeploymentManifest manifest) throws IOException {
		logger.debug(LOG_ENTRY, "setDeploymentManifest", manifest);
		DeploymentManifest old = getDeploymentManifest();
		// TODO Add to constants.
		File file = new File(directory, "OSGI-INF");
		if (!file.exists() && !file.mkdir())
			throw new IOException("Unable to make directory for "
					+ file.getCanonicalPath());
		file = new File(file, "DEPLOYMENT.MF");
		OutputStream out = new FileOutputStream(file);
		try {
			manifest.write(out);
			if (old != null)
				resources.remove(old);
			resources.put(manifest, file.toURI().toURL());
		} finally {
			out.close();
		}
		logger.debug(LOG_EXIT, "setDeploymentManifest");
	}
	
	public synchronized void setSubsystemManifest(SubsystemManifest manifest) throws IOException {
		logger.debug(LOG_ENTRY, "setSubsystemManifest", manifest);
		SubsystemManifest old = getSubsystemManifest();
		// TODO Add to constants.
		File file = new File(directory, "OSGI-INF");
		if (!file.exists() && !file.mkdir())
			throw new IOException("Unable to make directory for " + file.getCanonicalPath());
		file = new File(file, "SUBSYSTEM.MF");
		OutputStream out = new FileOutputStream(file);
		try {
			manifest.write(out);
			if (old != null)
				resources.remove(old);
			resources.put(manifest, file.toURI().toURL());
		}
		finally {
			out.close();
		}
		logger.debug(LOG_EXIT, "setSubsystemManifest");
	}
	
	private Resource createResource(File file) throws Exception {
		if (file.isDirectory() && file.getName().startsWith("subsystem"))
			return new SubsystemArchive(file);
		if (file.getName().endsWith(".jar"))
			return BundleResource.newInstance(file.toURI().toURL());
		if (!file.getName().startsWith("subsystem") && file.getName().endsWith(".ssa"))
			return new SubsystemFileResource(file);
		if (file.getName().endsWith("SUBSYSTEM.MF"))
			return new SubsystemManifest(file);
		if (file.getName().endsWith("DEPLOYMENT.MF"))
			return new DeploymentManifest(file);
		if (file.getName().endsWith(DataFile.IDENTITY_TYPE))
			return new DataFile(file);
		if (file.getName().endsWith(StaticDataFile.IDENTITY_TYPE))
			return new StaticDataFile(file);
		logger.warn("Ignoring unsupported resource type: " + file.getAbsolutePath());
		return null;
	}
	
	private void processDirectory(File content) throws Exception {
		logger.debug(LOG_ENTRY, "processDirectory", content);
		for (File file : content.listFiles()) {
			logger.debug("Processing file {}", file);
			processFile(file);
		}
		logger.debug(LOG_EXIT, "processDirectory");
	}
	
	private void processFile(File content) throws Exception {
		logger.debug(LOG_ENTRY, "processFile", content);
		if (content.isDirectory() && "OSGI-INF".equals(content.getName())) {
			processDirectory(content);
		}
		else {
			Resource resource = createResource(content);
			if (resource != null) {
				resources.put(resource, content.toURI().toURL());
			}
		}
		logger.debug(LOG_EXIT, "processFile");
	}
}
