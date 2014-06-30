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
import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.filesystem.IDirectory;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallAction implements PrivilegedAction<BasicSubsystem> {
	private final IDirectory content;
	private final AccessControlContext context;
	private final InputStream deploymentManifest;
	private final String location;
	private final BasicSubsystem parent;
	private static final Logger logger = LoggerFactory.getLogger(InstallAction.class);
	
	public InstallAction(String location, IDirectory content, BasicSubsystem parent, AccessControlContext context, InputStream deploymentManifest) {
		this.location = location;
		this.content = content;
		this.parent = parent;
		this.context = context;
		this.deploymentManifest = deploymentManifest;
	}
	
	@Override
	public BasicSubsystem run() {
		// Initialization of a null coordination must be privileged and,
		// therefore, occur in the run() method rather than in the constructor.
		Coordination coordination = Utils.createCoordination(parent);
		BasicSubsystem result = null;
		try {
			TargetRegion region = new TargetRegion(parent);
			SubsystemResource ssr = new SubsystemResource(location, content, parent);
			result = Activator.getInstance().getSubsystems().getSubsystemByLocation(location);
			if (result != null) {
				checkLifecyclePermission(result);
				if (!region.contains(result))
					throw new SubsystemException("Location already exists but existing subsystem is not part of target region: " + location);
				if (!(result.getSymbolicName().equals(ssr.getSubsystemManifest().getSubsystemSymbolicNameHeader().getSymbolicName())
						&& result.getVersion().equals(ssr.getSubsystemManifest().getSubsystemVersionHeader().getVersion())
						&& result.getType().equals(ssr.getSubsystemManifest().getSubsystemTypeHeader().getType())))
					throw new SubsystemException("Location already exists but symbolic name, version, and type are not the same: " + location);
				return (BasicSubsystem)ResourceInstaller.newInstance(coordination, result, parent).install();
			}
			result = (BasicSubsystem)region.find(
					ssr.getSubsystemManifest().getSubsystemSymbolicNameHeader().getSymbolicName(), 
					ssr.getSubsystemManifest().getSubsystemVersionHeader().getVersion());
			if (result != null) {
				checkLifecyclePermission(result);
				if (!result.getType().equals(ssr.getSubsystemManifest().getSubsystemTypeHeader().getType()))
					throw new SubsystemException("Subsystem already exists in target region but has a different type: " + location);
				return (BasicSubsystem)ResourceInstaller.newInstance(coordination, result, parent).install();
			}
			result = new BasicSubsystem(ssr, deploymentManifest);
			checkLifecyclePermission(result);
			return (BasicSubsystem)ResourceInstaller.newInstance(coordination, result, parent).install();
		}
		catch (Throwable t) {
			coordination.fail(t);
		}
		finally {
			try {
				coordination.end();
			}
			catch (CoordinationException e) {
				Throwable t = e.getCause();
				if (t instanceof SubsystemException)
					throw (SubsystemException)t;
				if (t instanceof SecurityException)
					throw (SecurityException)t;
				throw new SubsystemException(t);
			}
			finally {
				closeContentIfIClosable();
			}
		}
		return result;
	}

	private void closeContentIfIClosable() {
		//clean up temp file
		if (content instanceof ICloseableDirectory) {
			try{
				((ICloseableDirectory) content).close();
			}
			catch (IOException ioex) {
				logger.info("Exception calling close for content {}. Exception {}", 
						content, ioex);					
			}
		} 
	}
	
	private void checkLifecyclePermission(final BasicSubsystem subsystem) {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				SecurityManager.checkLifecyclePermission(subsystem);
				return null;
			}
		},
		context);
	}
}
