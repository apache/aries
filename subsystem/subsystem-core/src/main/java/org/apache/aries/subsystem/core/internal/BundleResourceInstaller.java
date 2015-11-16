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
package org.apache.aries.subsystem.core.internal;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.aries.util.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.subsystem.SubsystemException;

public class BundleResourceInstaller extends ResourceInstaller {
	/*
	 * Maps a BundleResource to a BundleRevision for the purpose of tracking
	 * any service requirements or capabilities. The instance is given to the
	 * Subsystems data structure as the constituent object.
	 * 
	 * The resource variable is allowed to be null so this class can be used
	 * when removing constituents from the data structure; however, note that
	 * service capabilities and requirements will not be available.
	 */
	static class BundleConstituent implements BundleRevision {
		private final Resource resource;
		private final BundleRevision revision;
		
		public BundleConstituent(Resource resource, BundleRevision revision) {
			if (resource instanceof BundleRevision) {
				try {
					this.resource = new BundleRevisionResource((BundleRevision)resource);
				}
				catch (SubsystemException e) {
					throw e;
				}
				catch (Exception e) {
					throw new SubsystemException(e);
				}
			}
			else
				this.resource = resource;
			this.revision = revision;
		}

		@Override
		public List<Capability> getCapabilities(String namespace) {
			List<Capability> result = new ArrayList<Capability>(revision.getCapabilities(namespace));
			if (resource != null && (namespace == null || ServiceNamespace.SERVICE_NAMESPACE.equals(namespace)))
				for (Capability capability : resource.getCapabilities(ServiceNamespace.SERVICE_NAMESPACE))
					result.add(new BasicCapability.Builder()
								.namespace(capability.getNamespace())
								.attributes(capability.getAttributes())
								.directives(capability.getDirectives())
								// Use the BundleRevision as the resource so it can be identified as a
								// runtime resource within the system repository.
								.resource(revision)
								.build());
			return Collections.unmodifiableList(result);
		}

		@Override
		public List<Requirement> getRequirements(String namespace) {
			List<Requirement> result = new ArrayList<Requirement>(revision.getRequirements(namespace));
			if (resource != null && (namespace == null || ServiceNamespace.SERVICE_NAMESPACE.equals(namespace)))
				for (Requirement requiremnet : resource.getRequirements(ServiceNamespace.SERVICE_NAMESPACE))
					result.add(new BasicRequirement.Builder()
								.namespace(requiremnet.getNamespace())
								.attributes(requiremnet.getAttributes())
								.directives(requiremnet.getDirectives())
								// Use the BundleRevision as the resource so it can be identified as a
								// runtime resource within the system repository.
								.resource(revision)
								.build());
			return Collections.unmodifiableList(result);
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof BundleConstituent))
				return false;
			BundleConstituent that = (BundleConstituent)o;
			return revision.equals(that.revision);
		}
		
		@Override
		public int hashCode() {
			int result = 17;
			result = 31 * result + revision.hashCode();
			return result;
		}

		@Override
		public Bundle getBundle() {
			return revision.getBundle();
		}
		
		public Resource getResource() {
			return resource;
		}
		
		public BundleRevision getRevision() {
			return revision;
		}

		@Override
		public String getSymbolicName() {
			return revision.getSymbolicName();
		}

		@Override
		public Version getVersion() {
			return revision.getVersion();
		}

		@Override
		public List<BundleCapability> getDeclaredCapabilities(String namespace) {
			return revision.getDeclaredCapabilities(namespace);
		}

		@Override
		public List<BundleRequirement> getDeclaredRequirements(String namespace) {
			return revision.getDeclaredRequirements(namespace);
		}

		@Override
		public int getTypes() {
			return revision.getTypes();
		}

		@Override
		public BundleWiring getWiring() {
			return revision.getWiring();
		}
		
		@Override
		public String toString() {
			return revision.toString();
		}
	}
	
	public BundleResourceInstaller(Coordination coordination, Resource resource, BasicSubsystem subsystem) {
		super(coordination, resource, subsystem);
	}
	
	public Resource install() {
		BundleRevision revision;
		if (resource instanceof BundleRevision) {
			revision = (BundleRevision)resource;
		}
		else if (resource instanceof BundleRevisionResource) {
		    revision = ((BundleRevisionResource)resource).getRevision();
		}
		else {
			try {
				revision = installBundle();
			}
			catch (Exception e) {
				throw new SubsystemException(e);
			}
		}
		addReference(revision);
		addConstituent(new BundleConstituent(resource, revision));
		return revision;
	}
	
	private BundleRevision installBundle() throws Exception {
		final Bundle bundle;
		Method getContent = resource.getClass().getMethod("getContent");
		getContent.setAccessible(true);
		InputStream is = (InputStream)getContent.invoke(resource);
		ThreadLocalSubsystem.set(provisionTo);
		try {
			bundle = provisionTo.getRegion().installBundleAtLocation(getLocation(), is);
		}
		catch (BundleException e) {
			throw new SubsystemException(e);
		}
		finally {
			ThreadLocalSubsystem.remove();
			// Although Region.installBundle ultimately calls BundleContext.install,
			// which closes the input stream, an exception may occur before this
			// happens. Also, the Region API does not guarantee the stream will
			// be closed.
			IOUtils.close(is);
		}
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				// Nothing
			}

			public void failed(Coordination coordination) throws Exception {
				bundle.uninstall();
			}
		});
		// Set the start level of all bundles managed (i.e. installed) by the
		// subsystems implementation to 1 in case the framework's default bundle
		// start level has been changed. Otherwise, start failures will occur
		// if a subsystem is started at a lower start level than the default.
		// Setting the start level does no harm since all managed bundles are 
		// started transiently anyway.
		bundle.adapt(BundleStartLevel.class).setStartLevel(1);
		return bundle.adapt(BundleRevision.class);
	}
}
