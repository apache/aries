/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.archive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.aries.subsystem.core.resource.ResourceFactory;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Requirement;
import org.osgi.framework.wiring.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.SubsystemException;

public class Archive implements Repository {
	private DeploymentManifest deploymentManifest;
	private SubsystemManifest subsystemManifest;
	
//	private final DeploymentManifest deploymentManifest;
	private final File directory;
	private final Map<Resource, URL> resources = new HashMap<Resource, URL>();
//	private final SubsystemManifest subsystemManifest;
	
	public Archive(String location, File dir, InputStream content) throws IOException, URISyntaxException {
		directory = dir;
		checkDirectory();
		processArchive(content);
		processResources();
		// TODO Add to constants.
		File file = new File(dir, "OSGI-INF/SUBSYSTEM.MF");
		if (file.exists())
			subsystemManifest = new SubsystemManifest(file);
		// TODO Add to constants.
		file = new File(directory, "OSGI-INF/DEPLOYMENT.MF");
		if (file.exists())
			deploymentManifest = new DeploymentManifest(file);	
	}
	
//	public Archive(String location, File dir, InputStream content, SubsystemEnvironment environment) throws IOException, URISyntaxException {
//		directory = dir;
//		checkDirectory();
//		processArchive(content);
//		processResources();
//		// TODO Add to constants.
//		File file = new File(dir, "OSGI-INF/SUBSYSTEM.MF");
//		if (!file.exists()) {
//			SubsystemUri uri = new SubsystemUri(location);
//			subsystemManifest = SubsystemManifest.newInstance(uri.getSymbolicName(), uri.getVersion(), resources);
//			OutputStream out = new FileOutputStream(file);
//			try {
//				subsystemManifest.write(out);
//			}
//			finally {
//				out.close();
//			}
//		}
//		else
//			subsystemManifest = new SubsystemManifest(file);
//		// TODO Add to constants.
//		file = new File(directory, "OSGI-INF/DEPLOYMENT.MF");
//		if (!file.exists()) {
//			deploymentManifest = DeploymentManifest.newInstance(subsystemManifest, environment);
//			OutputStream out = new FileOutputStream(file);
//			try {
//				deploymentManifest.write(out);
//			}
//			finally {
//				out.close();
//			}
//		}
//		else
//			deploymentManifest = new DeploymentManifest(file);
//	}
	
	@Override
	public Collection<Capability> findProviders(Requirement requirement) {
		Collection<Capability> capabilities = new ArrayList<Capability>();
		for (Resource resource : getResources())
			for (Capability capability : resource.getCapabilities(requirement.getNamespace()))
				if (requirement.matches(capability))
					capabilities.add(capability);
		return capabilities;
	}

	@Override
	public URL getContent(Resource resource) {
		return resources.get(resource);
	}
	
	public synchronized DeploymentManifest getDeploymentManifest() {
		return deploymentManifest;
	}
	
	public File getDirectory() {
		return directory;
	}
	
	public synchronized SubsystemManifest getSubsystemManifest() {
		return subsystemManifest;
	}
	
	public Collection<Resource> getResources() {
		return Collections.unmodifiableCollection(resources.keySet());
	}
	
	public Collection<String> getResourceNames() {
		File[] files = directory.listFiles();
		ArrayList<String> result = new ArrayList<String>(files.length);
		for (File file : files) {
			if (file.isDirectory()) continue;
			result.add(file.getName());
		}
		result.trimToSize();
		return result;
	}
	
	public URL getURL(String resource) throws IOException {
		return new File(directory, resource).toURI().toURL();
	}
	
	public synchronized void setDeploymentManifest(DeploymentManifest manifest) throws IOException {
		// TODO Add to constants.
		OutputStream out = new FileOutputStream(new File(directory, "OSGI-INF/DEPLOYMENT.MF"));
		try {
			manifest.write(out);	
			deploymentManifest = manifest;
		}
		finally {
			out.close();
		}
	}
	
	public synchronized void setSubsystemManifest(SubsystemManifest manifest) throws IOException {
		// TODO Add to constants.
		OutputStream out = new FileOutputStream(new File(directory, "OSGI-INF/SUBSYSTEM.MF"));
		try {
			manifest.write(out);	
			subsystemManifest = manifest;
		}
		finally {
			out.close();
		}
	}
	
	private void checkDirectory() {
		if (directory.exists() && !directory.delete())
			throw new SubsystemException("The given subsystem archive directory already exists and cannot be deleted: " + directory);
		if (directory.isFile())
			throw new SubsystemException("The given subsystem archive directory is not a directory: " + directory);
		if (!directory.mkdirs())
			throw new SubsystemException("The given subsystem archive directory could not be created: " + directory);
	}
	
	private void processArchive(InputStream content) throws IOException {
		ZipInputStream zis = new ZipInputStream(content);
		try {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				processEntry(directory, zis, entry);
			}
		}
		catch (IOException e) {
			directory.delete();
			throw e;
		}
		catch (RuntimeException e) {
			directory.delete();
			throw e;
		}
		finally {
			try {
				zis.close();
			}
			catch (IOException e) {}
		}
	}
	
	private void processEntry(File dir, ZipInputStream zis, ZipEntry entry) throws IOException {
		File file = new File(dir, entry.getName());
		if (entry.isDirectory()) {
			if (!file.mkdirs())
				throw new SubsystemException("Failed to create resource directory: " + file);
			return;
		}
		if (file.exists())
			throw new SubsystemException("Resource already exists: " + file);
		processFile(file, zis);
	}
	
	private void processFile(File file, ZipInputStream zis) throws IOException {
		OutputStream fos = new FileOutputStream(file);
		try {
			byte[] bytes = new byte[1024];
			int read;
			while ((read = zis.read(bytes)) != -1)
				fos.write(bytes, 0, read);
		}
		finally {
			try {
				fos.close();
			}
			catch (IOException e) {}
		}
	}
	
	private void processResources() throws IOException, URISyntaxException {
		for (String name : getResourceNames()) {
			URL url = getURL(name);
			resources.put(new ResourceFactory().newResource(url), url);
		}
	}
}
