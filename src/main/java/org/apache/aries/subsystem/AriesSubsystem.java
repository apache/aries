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
package org.apache.aries.subsystem;

import java.io.InputStream;
import java.util.Collection;

import org.apache.aries.util.filesystem.IDirectory;
import org.osgi.resource.Requirement;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;
import org.osgi.service.subsystem.SubsystemPermission;

public interface AriesSubsystem extends Subsystem {
	/**
	 * Adds the specified requirements to this subsystem's sharing policy.
	 * <p>
	 * The sharing policy of this subsystem's region is updated with the 
	 * specified requirements (i.e. imports). Requirements already part of the
	 * sharing policy are ignored. Upon return, constituents of this subsystem 
	 * will be allowed to resolve against matching capabilities that are visible 
	 * to the parent subsystems.
	 *
	 * @param requirement The requirement to add to the sharing policy.
	 * @throws SubsystemException If the requirement did not already exist and
	 *         could not be added.
	 * @throws UnsupportedOperationException If this is the root subsystem or 
	 *         the type does not support additional requirements.
	 */
	void addRequirements(Collection<Requirement> requirements);

	@Override
	AriesSubsystem install(String location);

	@Override
	AriesSubsystem install(String location, InputStream content);
	
	/**
	 * Installs a subsystem from the specified location identifier and content.
	 * <p>
	 * This method performs the same function as calling
	 * {@link #install(String, InputStream)} except the content is retrieved
	 * from the specified {@link IDirectory} instead.
	 * 
	 * @param location The location identifier of the subsystem to install.
	 * @param content The directory from which this subsystem will be read or
	 *        {@code null} to indicate the directory must be created from the
	 *        specified location identifier.
	 * @return The installed subsystem.
	 * @throws IllegalStateException If this subsystem's state is in
	 *         {@link State#INSTALLING INSTALLING}, {@link State#INSTALL_FAILED
	 *         INSTALL_FAILED}, {@link State#UNINSTALLING UNINSTALLING},
	 *         {@link State#UNINSTALLED UNINSTALLED}.
	 * @throws SubsystemException If the installation failed.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@link SubsystemPermission}[installed subsystem,LIFECYCLE], and
	 *         the runtime supports permissions.
	 * @see #install(String, InputStream)
	 */
	AriesSubsystem install(String location, IDirectory content);
}
