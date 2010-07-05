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
 * A representation of a subsystem in the framework. A subsystem is a
 * collection of bundles and/or other resource. A subsystem has isolation
 * semantics. Subsystem types are defined that have different default isolation
 * semantics. For example, an Application subsystem does not export any of the
 * packages or services provided by its content bundles, and imports any
 * packages or services that are required to satisfy unresolved package or
 * service dependencies of the content bundles.
 * 
 * A subsystem is defined using a manifest format.
 */
public interface Subsystem {

    /**
     * The states of a subsystem in the framework. These states match those of a
     * Bundle and are derived using the same rules as CompositeBundles. As such,
     * they are more a reflection of what content bundles are permitted to do
     * rather than an aggregation of the content bundle states.
     */
    public enum State {

        /**
         * A subsystem is in the INSTALLED state when it is initially created.
         * It may or may not contain content bundles. No content bundles are
         * permitted to be resolved while the subsystem is in the INSTALLED
         * state.
         */
        INSTALLED,

        /**
         * A subsystem in the RESOLVED is allowed to have its content bundles
         * resolved. The content bundles are not allowed to activate while the
         * subsystem is in the RESOLVED state.
         */
        RESOLVED,

        /**
         * A subsystem is in the STARTING state when all its content bundles are
         * enabled for activation. The content bundles are started according to
         * the start-level specification.
         */
        STARTING,

        /**
         * A subsystem in the STOPPING state is in the process of taking its its active start level to
         * zero, stopping all the content bundles.
         */
        STOPPING,

        /**
         * A subsystem is in the ACTIVE state when it has reached the beginning
         * start-level (for starting it's contents), and all its persistently
         * started content bundles that are resolved and have had their
         * start-levels met have completed, or failed, their activator start
         * method.
         */
        ACTIVE,

        /**
         * A subsystem is in the UNINSTALLED state when all its content bundles
         * and uninstalled and its system bundle context is invalidated.
         */
        UNINSTALLED

    }

    /**
     * Gets the state of the subsystem.
     *
     * @return the state of the subsystem.
     */
    State getState();

    /**
     * Starts the subsystem. The subsystem is started according to the rules
     * defined for Bundles and the content bundles are enabled for activation.
     * 
     * @throws SubsystemException
     *             If this subsystem could not be started.
     * @throws IllegalStateException
     *             If this subsystem has been uninstalled.
     * @throws SecurityException
     *             If the caller does not have the appropriate
     *             AdminPermission[this,EXECUTE] and the runtime supports
     *             permissions.
     */
    void start() throws SubsystemException;

    /**
     * Stops the subsystem. The subsystem is stopped according to the rules
     * defined for Bundles and the content bundles are disabled for activation
     * and stopped.
     * 
     * @throws SubsystemException
     *             TODO: does not fit with BundleException from Bundle.stop.
     *             What are the circumstances that cause this to be throw? When
     *             an exception is thrown from a content bundle?
     * @throws IllegalStateException
     *             If this subsystem has been uninstalled.
     * @throws SecurityException
     *             If the caller does not have the appropriate
     *             AdminPermission[this,EXECUTE] and the runtime supports
     *             permissions.
     */
    void stop() throws SubsystemException;

    /**
     * Gets the identifier of the subsystem. Subsystem identifiers are assigned
     * when the subsystem is installed and are unique within the framework.
     * 
     * @return the identifier for the subsystem.
     */
    long getSubsystemId();

    /**
     * The location identifier used to install this subsystem through
     * SubsystemAdmin.install. This identifier does not change while this
     * subsystem remains installed, even after SubsystemAdmin.update. This
     * location identifier is used in SubsystemAdmin.update if no other update
     * source is specified.
     * 
     * @return The string representation of the subsystem's location identifier.
     */
    String getLocation();

    /**
     * Gets the symbolic name of this subsystem.
     *
     * @return the symbolic name of this subsystem.
     */
    String getSymbolicName();

    /**
     * Gets the version of this subsystem.
     *
     * @return the version of this subsystem.
     */
    Version getVersion();

    /**
     * Gets the headers used to define this subsystem. The headers will be
     * localized using the locale returned by java.util.Locale.getDefault. This
     * is equivalent to calling <code>getHeaders(null)</code>.
     * 
     * @return the headers used to define this subsystem.
     * @throws SecurityException
     *             If the caller does not have the appropriate
     *             AdminPermission[this,METADATA] and the runtime supports
     *             permissions.
     */
    Map<String, String> getHeaders();

    /**
     * Gets the headers used to define this subsystem.
     * 
     * @param locale
     *            The locale name to be used to localize the headers. If the
     *            locale is <code>null</code> then the locale returned by
     *            java.util.Locale.getDefault is used. If the value is the empty
     *            string then the returned headers are returned unlocalized.
     * @return the headers used to define this subsystem, localized to the
     *         specified locale.
     * @throws SecurityException
     *             If the caller does not have the appropriate
     *             AdminPermission[this,METADATA] and the runtime supports
     *             permissions.
     */
    Map<String, String> getHeaders(String locale);

    /**
     * Gets the content bundles of this subsystem.
     *
     * @return the content of this subsystem.
     */
    Collection<Bundle> getConstituents();

}
