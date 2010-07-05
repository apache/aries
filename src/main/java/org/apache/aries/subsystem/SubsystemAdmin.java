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

import org.osgi.framework.Version;

/**
 * Subsystem administration interface.
 * 
 * A service implementing this interface will be registered in the OSGi service
 * registry. This service provides the capability to manage subsystem (e.g.
 * install, uninstall, update). Each instance of the service manages subsystems
 * at a single level. For example, if subsystems are hierarchically nested, then
 * an instance of this service is used to manage the subsystems at each level in
 * the hierarchy.
 * 
 * Install / update / uninstall operations will be performed sequentially, i.e.
 * they will be synchronized so that no two operations will be performed at the
 * same time.
 * 
 */
public interface SubsystemAdmin {

    /**
     * Gets the subsystems managed by this service. This only includes the
     * top-level Subsystems installed in the Framework, CoompositeBundle or
     * Subsystem from which this service has been retrieved.
     * 
     * @return the Subsystems managed by this service.
     */
    Collection<Subsystem> getSubsystems();

    /**
     * Gets the Subsystem identified by its id.
     * 
     * @param id
     *            The id of the Subsystem to get.
     * @return the Subsystem matching the given id.
     */
    Subsystem getSubsystem(long id);

    /**
     * Gets a Subsystem given its symbolic name and version.
     * 
     * @param symbolicName
     *            The symbolic name of the subsystem to be returned.
     * @param version
     *            The version of the Subsystem to be returned.
     * @return the Subsystem matching the symbolic name and version or
     *         <code>null</code> if no match is found.
     */
    Subsystem getSubsystem(String symbolicName, Version version);

    /**
     * Install a new subsystem from the specified location identifier.
     * 
     * This method performs the same function as calling {@link #install(String, InputStream)} with the specified 
     * location identifier and a null InputStream.
     *
     * @param location
     * @return
     */
    Subsystem install(String location) throws SubsystemException;

    /**
     * Install a new subsystem from the specified <code>InputStream</code>
     * object.
     * 
     * If the specified <code>InputStream</code> is <code>null</code>, the
     * <code>InputStream</code> must be created from the specified location.
     * 
     * The specified location identifier will be used as the identity of the
     * subsystem. Every installed subsystem is uniquely identified by its
     * location identifier which is typically in the form of a URL.
     * 
     * A subsystem and its contents must remain installed across Framework and
     * VM restarts. The subsystem itself is installed atomically, however its
     * contents are not.
     * 
     * The following steps are required to install a subsystem:
     * 
     * <ol>
     * <li> If a subsystem containing the same location identifier is already
     *    installed, the <code>Subsystem</code> object for that subsystem is
     *    returned. </li>
     * <li> The subsystem's content is read from the input stream. If
     *    this fails, a <code>SubsystemException</code> is thrown.</li> 
     * <li> The empty subsystem object is created and assigned a unique id 
     *    which is higher than any previous bundle of subsystem identifier.</li> 
     * <li> The subsystem's associated resources are located. </li>
     * <li> The subsystem's state is set to <code>INSTALLED</code> </li>
     * <li> The subsystem event of type <code>INSTALLED</code> is fired.</li> 
     * <li> Installation of subsystem content is started. </li>
     * <li> The subsystem object for the newly installed subsystem is returned.</li>
     * </ol>
     * 
     * TODO: discuss the above steps.
     * 
     * @param location
     *            The location identifier of the subsystem to be installed.
     * @param content
     *            The <code>InputStream</code> from where the subsystem is to be
     *            installed or <code>null</code> if the location is to be used
     *            to create the <code>InputStream</code>.
     * @return the installed subsystem.
     * @throws SubsystemException
     *             If the <code>InputStream</code> cannot be read or the
     *             installation fails. This exception is not thrown in the event
     *             of the subsystems contents failing to install.
     * @throws SecurityException
     *             If the caller does not have the appropriate
     *             AdminPermission[installed subsystem,LIFECYCLE], and the Java
     *             Runtime Environment supports permissions.
     */
    Subsystem install(String location, InputStream content) throws SubsystemException;

    /**
     * Update the given subsystem.
     *
     * This method performs the same function as calling {@link #update(Subsystem, InputStream)} 
     * with the specified subsystem and a <code>null</code> <code>InputStream</code>.
     * 
     * @param subsystem The subsystem to be updated.
     */
    void update(Subsystem subsystem) throws SubsystemException;

    /**
     * Update the given subsystem from an <code>InputStream</code>.
     * 
     * If the specified <code>InputStream</code> is <code>null</code>, the
     * InputStream must be created from the subsystem's
     * {@link SubsystemConstants#SUBSYSTEM_UPDATELOCATION
     * Subsystem-UpdateLocation} Manifest header if present, or this subsystem's
     * location provided when the subsystem was originally installed.
     * 
     * @param subsystem
     *            The subsystem to be updated.
     * @param content
     *            The <code>InputStream</code> from which to update the
     *            subsystem or <code>null</code> if the
     *            {@link SubsystemConstants#SUBSYSTEM_UPDATELOCATION
     *            Subsystem-UpdateLocation} or original location are to be used.
     * @throws SubsystemException
     *             If the <code>InputStream</code> cannot be read of the update
     *             fails.
     * @throws IllegalStateException
     *             If the subsystem is in the <code>UNINSTALLED</code> state.
     * @throws SecurityException
     *             If the caller does not have the appropriate
     *             AdminPermission[this,LIFECYCLE] for both the current
     *             subsystem and the updated subsystem and the Java Runtime
     *             Environment supports permissions.
     */
    void update(Subsystem subsystem, InputStream content) throws SubsystemException;

    /**
     * Uninstall the given subsystem.
     * 
     * This method causes the Framework to notify other bundles and subsystems
     * that this subsystem is being uninstalled, and then puts this subsystem
     * into the <code>UNINSTALLED</code> state. The Framework must remove any
     * resources related to this subsystem that it is able to remove. If this
     * subsystem has exported any packages, the Framework must continue to make
     * these packages available to their importing bundles or subsystems until
     * the Package-Admin.refreshPackages method has been called or the Framework
     * is relaunched.
     * 
     * The following steps are required to uninstall a subsystem: 
     * <ol>
     * <li> If this subsystem's state is UNINSTALLED then an IllegalStateException is thrown.</li>
     * <li> If this subsystem's state is ACTIVE, STARTING or STOPPING, this
     *    subsystem is stopped as described in the Subsystem.stop method. If
     *    Subsystem.stop throws an exception, a Framework event of type
     *    FrameworkEvent.ERROR is fired containing the exception. </li>
     * <li> This subsystem's state is set to UNINSTALLED. </li>
     * <li> A subsystem event of type SubsystemEvent.UNINSTALLED is fired.</li> 
     * <li> This subsystem and any persistent storage area provided for this subsystem 
     *    by the Framework are removed.</li>
     * </ol>
     * 
     * @param subsystem
     *            The subsystem to uninstall.
     * @throws SubsystemException
     *             If the uninstall failed.
     * @throws IlegalStateException
     *             If the subsystem is already in the <code>UNISTALLED</code>
     *             state.
     * @throws SecurityException
     *             If the caller does not have the appropriate
     *             AdminPermission[this,LIFECYCLE] and the Java Runtime
     *             Environment supports permissions.
     */
  void uninstall(Subsystem subsystem) throws SubsystemException;

    /**
     * Cancel the current operation.
     * 
     * The installing thread must throw a <code>SubsystemException</code> if the
     * operation has actually been cancelled and rolled back before completion.
     * 
     * @return <code>true</code> if an operation was ongoing and requested to be
     *         cancelled, <code>false</code> if there was no ongoing operation.
     * 
     *         TODO: discuss this. Why is it necessary?
     */
    boolean cancel();

}