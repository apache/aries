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

public class SubsystemConstants {

    /**
     * Private constructor to prevent objects of this type.
     */
    private SubsystemConstants() {
        // non-instantiable
    }

    //====================================================
    //
    //   Subsystem manifest headers
    //
    //====================================================

    /**
     * The subsystem manifest version header must be present and equals to 1.0
     * for this version of subsystems.
     */
    public static final String SUBSYSTEM_MANIFESTVERSION     = "Subsystem-ManifestVersion";

    /**
     * Symbolic name for the subsystem.  Must be present.
     */
    public static final String SUBSYSTEM_SYMBOLICNAME        = "Subsystem-SymbolicName";

    /**
     * Version of the subsystem.  If not present, the default value is 0.0.0.
     */
    public static final String SUBSYSTEM_VERSION             = "Subsystem-Version";

    /**
     * The human readable subsystem name.
     */
    public static final String SUBSYSTEM_NAME                = "Subsystem-Name";

    /**
     * The human readable subsystem description.
     */
    public static final String SUBSYSTEM_DESCRIPTION         = "Subsystem-Description";

    /**
     * Name of the resource to use for localized headers.
     */
    public static final String SUBSYSTEM_LOCALIZATION        = "Subsystem-Localization";

    /**
     * Location to use when updating the subsystem.
     */
    public static final String SUBSYSTEM_UPDATELOCATION      = "Subsystem-UpdateLocation";

    /**
     * The list of subsystem contents identified by a symbolic name and version.
     */
    public static final String SUBSYSTEM_CONTENT             = "Subsystem-Content";

    /**
     * TODO: what is this for?
     */
    public static final String SUBSYSTEM_RESOURCES           = "Subsystem-Resources";

    /**
     * The packages to be imported into the subsystem for use by the subsystem contents.
     */
    public static final String SUBSYSTEM_IMPORTPACKAGE       = "Subsystem-ImportPackage";

    /**
     * The packages to be exported by the subsystem for use outside the
     * subsystem. The packages declarations must match a package provide by a
     * bundle listed in the subsystem content.
     */
    public static final String SUBSYSTEM_EXPORTPACKAGE       = "Subsystem-ExportPackage";

    /**
     * A list of service filters used to identify services that this subsystem requires.
     */
    public static final String SUBSYSTEM_IMPORTSERVICE       = "Subsystem-ImportService";

    /**
     * A list of service filters used to identify the services provided by
     * bundles inside the subsystem that can be exported outside the subsystem.
     */
    public static final String SUBSYSTEM_EXPORTSERVICE       = "Subsystem-ExportService";
    
    /**
     * A list of Bundles the subsystem requires. These bundles are made
     * available to satisfy Require-Bundle statements for the subsystem content
     * bundles.
     */
    public static final String SUBSYSTEM_REQUIREBUNDLE       = "Subsystem-RequireBundle";

    //====================================================
    //
    //   Application manifest headers
    //
    //====================================================

    /**
     * The subsystem manifest version header must be present and equals to 1.0
     * for this version of applications.
     */
    public static final String APPLICATION_MANIFESTVERSION   = "Application-ManifestVersion";

    /**
     * Symbolic name for the application.  Must be present.
     */
    public static final String APPLICATION_SYMBOLICNAME      = "Application-SymbolicName";

    /**
     * Version of the application.  If not present, the default value is 0.0.0.
     */
    public static final String APPLICATION_VERSION           = "Application-Version";

    /**
     * Human readable application name.
     */
    public static final String APPLICATION_NAME              = "Application-Name";

    /**
     * Human readable description.
     */
    public static final String APPLICATION_DESCRIPTION       = "Application-Description";

    /**
     * Name of the resource to use for localized headers.
     */
    public static final String APPLICATION_LOCALIZATION      = "Application-Localization";

    /**
     * Location to use when updating the application.
     */
    public static final String APPLICATION_UPDATELOCATION    = "Application-UpdateLocation";

    /**
     * The list of application contents identified by a symbolic name and version.
     */
    public static final String APPLICATION_CONTENT           = "Application-Content";
    
    /**
     * TODO: what is this for?
     */
    public static final String APPLICATION_RESOURCES         = "Application-Resources";


    //====================================================
    //
    //   Library manifest headers
    //
    //====================================================

    /**
     * The subsystem manifest version header must be present and equals to 1.0
     * for this version of applications.
     */
    public static final String LIBRARY_MANIFESTVERSION       = "Library-ManifestVersion";

    /**
     * Symbolic name for the application.  Must be present.
     */
    public static final String LIBRARY_SYMBOLICNAME          = "Library-SymbolicName";

    /**
     * Version of the application.  If not present, the default value is 0.0.0.
     */
    public static final String LIBRARY_VERSION               = "Library-Version";

    /**
     * Human readable library name.
     */
    public static final String LIBRARY_NAME                  = "Library-Name";

    /**
     * Human readable library description.
     */
    public static final String LIBRARY_DESCRIPTION           = "Library-Description";

    /**
     * Name of the resource to use for localized headers
     */
    public static final String LIBRARY_LOCALIZATION          = "Library-Localization";

    /**
     * Location to use when updating the library
     */
    public static final String LIBRARY_UPDATELOCATION        = "Library-UpdateLocation";

    /**
     * The list of library contents identified by a symbolic name and version.
     */
    public static final String LIBRARY_CONTENT               = "Library-Content";
    
    /**
     * TODO: what is this for.
     */
    public static final String LIBRARY_RESOURCES             = "Library-Resources";


    //====================================================
    //
    //   Attributes used in headers
    //
    //====================================================

    /**
     * Directive on the manifest header of a composite indicating the
     * composite is managed as a subsystem.
     */
    public static final String SUBSYSTEM_DIRECTIVE           = "subsystem";

    /**
     * Service property to be set on {@link org.apache.aries.subsystem.spi.ResourceProcessor}
     * services to indicate which types of resource it can process.
     */
    public static final String SERVICE_RESOURCE_TYPE         = "resource-type";

    /**
     * Attribute to indicate the type on a resource
     */
    public static final String RESOURCE_TYPE_ATTRIBUTE       = "type";

    /**
     * Identify resources that are bundles (this is the default type).
     */
    public static final String RESOURCE_TYPE_BUNDLE          = "bundle";

    /**
     * Identify resources that are subsystems.
     */
    public static final String RESOURCE_TYPE_SUBSYSTEM       = "subsystem";

    /**
     * Attribute to indicate the location of a resource
     */
    public static final String RESOURCE_LOCATION_ATTRIBUTE   = "location";

    /**
     * Attribute to indicate a bundle needs to be started, defaults to <code>true</code>
     */
    public static final String RESOURCE_START_ATTRIBUTE      = "start";

    /**
     * Attribute to indicate a bundle needs to be forced updated, even if the version is the same, defaults to <code>false</code>
     */
    public static final String RESOURCE_UPDATE_ATTRIBUTE     = "update";

    /**
     * Attribute to indicate the start level that must be associated to a constituent bundle or subsystem 
     */
    public static final String RESOURCE_START_LEVEL_ATTRIBUTE      = "start-level";


}
