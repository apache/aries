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

import java.util.Collection;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Interface representing an installed subsystem
 */
public interface Subsystem {

    public enum State {

        INSTALLED,
        RESOLVED,
        STARTING,
        STOPPING,
        ACTIVE,
        UNINSTALLED

    }

    /**
     * Retrieve the state of the subsystem.
     *
     * @return
     */
    State getState();

    /**
     * Start the subsystem (i.e. start all its constituent bundles according to their start level).
     */
    void start();

    /**
     * Stop the subsystem (i.e. stop all its constituent bundles).
     */
    void stop();

    /**
     * The identifier of the subsystem.  Must be unique in the framework.
     *
     * @return
     */
    long getSubsystemId();

    /**
     * The location of the subsystem.
     * The location will be used when updating a subsystem to load the new
     * content and/or identify subsystems to update.
     *
     * @return
     */
    String getLocation();

    /**
     * Retrieve the symbolic name of this subsystem.
     *
     * @return
     */
    String getSymbolicName();

    /**
     * Retrieve the version of this subsystem.
     *
     * @return
     */
    Version getVersion();

    /**
     * Return the subsystem headers
     *
     * @return
     */
    Map<String, String> getHeaders();

    /**
     * Return the subsystem headers
     *
     * @return
     */
    Map<String, String> getHeaders(String locale);

    /**
     * Retrieve the constituent bundles of this subsystem.
     *
     * @return
     */
    Collection<Bundle> getConstituents();

}
