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
     * Retrieve all subsystems managed by this service
     * @return
     */
    Map<Long, Subsystem> getSubsystems();

    /**
     * Retrieve a subsystem given its scope.
     *
     * @param scope
     * @return
     */
    Subsystem getSubsystem(String scope);

    /**
     * Install a new subsystem from the specified location identifier.
     * 
     * This method performs the same function as calling <code>install(String, InputStream)</code> with the specified 
     * location identifier and a null InputStream.
     *
     * @param location
     * @return
     */
    Subsystem install(String location) throws SubsystemException;

    /**
     * Install a new subsystem from the specified InputStream object.  If the specified InputStream is null,
     * the InputStream must be created from the specified location.
     * 
     * The specified location identifier will be used as the identity of the subsystem. 
     * Every installed subsystem is uniquely identified by its location identifier which is typically in the form of a URL.
     * 
     * If a subsystem containing the same location identifier is already installed, the Subsystem object for that subsystem is returned.
     * 
     * @param location
     * @param content
     * @return
     */
    Subsystem install(String location, InputStream content) throws SubsystemException;

    /**
     * Update the given subsystem.
     *
     * The updated subsystem metadata will be loaded from
     * the subsystem location, or from the subsystem update location
     * if specified.
     *
     * @param subsystem
     */
    void update(Subsystem subsystem) throws SubsystemException;

    /**
     * Update the given subsystem
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
     * Force the uninstallation of a subsystem.
     * Any errors will be ignored.
     *
     * @param subsystem
     */
    void uninstallForced(Subsystem subsystem);

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