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
import java.util.Map;

import org.osgi.framework.Version;

/**
 * Subsystems administration interface.
 *
 * A service implementing this interface will be registered in the OSGi registry.
 *
 * Install / update / uninstall operations will be performed sequentially, i.e.
 * they will be synchronized so that no two operations will be performed at the
 * same time.
 *
 * This service will also be made available from managed subsystems, so that only subsystems
 * from the current framework will be available, not nested frameworks.
 *
 */
public interface SubsystemAdmin {

    /**
     * Retrieve all subsystems managed by this service.
     * This includes all the top-level subsystems installed in the composite
     * which this service has been retrieved from.
     *
     * @return
     */
    Collection<Subsystem> getSubsystems();

    /**
     * Retrieve a subsystem given its id.
     *
     * @param id
     * @return
     */
    Subsystem getSubsystem(long id);

    /**
     * Retrieve a subsystem given its symbolic name and id
     *
     * @param symbolicName
     * @param version
     * @return
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
     * Install a new subsystem from the specified <code>InputStream</code> object.  
     * 
     * If the specified <code>InputStream</code> is <code>null</code>,
     * the <code>InputStream</code> must be created from the specified location.
     * 
     * The specified location identifier will be used as the identity of the subsystem. 
     * Every installed subsystem is uniquely identified by its location identifier which is typically in the form of a URL.
     * 
     * The following steps are required to install a subsystem:
     * 
     * 1. If a subsystem containing the same location identifier is already installed, the <code>Subsystem</code> object for that subsystem is returned.
     * 2. The subsystem's content is read from the input stream.  If this fails, a SubsystemException is thrown.
     * 3. The subsystem's associated resources are located
     * 4. The subsystem's state is set to <code>INSTALLED</code>
     * 5. The subsystem event of type INSTALLED is fired.
     * 6. The subsystem object for the newly installed subsystem is returned
     * 
     * @param location
     * @param content
     * @return
     */
    Subsystem install(String location, InputStream content) throws SubsystemException;

    /**
     * Update the given subsystem.
     *
     * This method performs the same function as calling {@link #update(Subsystem, InputStream)} 
     * with the specified subsystem and a <code>null</code> InputStream.
     * 
     * @param subsystem
     */
    void update(Subsystem subsystem) throws SubsystemException;

    /**
     * Update the given subsystem from an <code>InputStream</code>.  
     * 
     * If the specified <code>InputStream</code> is <code>null</code>, the InputStream must be created from
     * the subsystem's {@link SubsystemConstants#SUBSYSTEM_UPDATELOCATION Subsystem-UpdateLocation} Manifest header 
     * if present, or this subsystem's original location.
     *
     * @param subsystem
     * @param content
     */
    void update(Subsystem subsystem, InputStream content) throws SubsystemException;

    /**
     * Uninstall the given subsystem.
     *
     * @param subsystem
     */
    void uninstall(Subsystem subsystem) throws SubsystemException;

    /**
     * Abort the current operation.
     * The installing thread must throw a SubsystemException if the operation
     * has actually been canceled and rolled back before terminating.
     *
     * @return <code>true</code> if an operation was ongoing and requested to be
     *    cancelled, <code>false</code> if there was no ongoing operation
     */
    boolean cancel();

}