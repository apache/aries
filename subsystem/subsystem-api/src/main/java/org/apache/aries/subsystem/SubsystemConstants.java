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

    public static final String SUBSYSTEM_MANIFESTVERSION = "Subsystem-ManifestVersion";
    public static final String SUBSYSTEM_SYMBOLICNAME    = "Subsystem-SymbolicName";
    public static final String SUBSYSTEM_VERSION         = "Subsystem-Version";
    public static final String SUBSYSTEM_NAME            = "Subsystem-Name";
    public static final String SUBSYSTEM_DESCRIPTION     = "Subsystem-Description";
    public static final String SUBSYSTEM_CONTENT         = "Subsystem-Content";
    public static final String SUBSYSTEM_IMPORTPACKAGE   = "Subsystem-ImportPackage";
    public static final String SUBSYSTEM_EXPORTPACKAGE   = "Subsystem-ExportPackage";
    public static final String SUBSYSTEM_IMPORTSERVICE   = "Subsystem-ImportService";
    public static final String SUBSYSTEM_EXPORTSERVICE   = "Subsystem-ExportService";
    public static final String SUBSYSTEM_REQUIREBUNDLE   = "Subsystem-RequireBundle";
    public static final String SUBSYSTEM_LOCALIZATION    = "Subsystem-Localization";
    public static final String SUBSYSTEM_UPDATELOCATION  = "Subsystem-UpdateLocation";

    /*
    String APPLICATION_SYMBOLICNAME = "Application-SymbolicName";
    String APPLICATION_VERSION = "Application-Version";
    String APPLICATION_NAME = "Application-Name";

    String LIBRARY_SYMBOLICNAME = "Library-SymbolicName";
    String LIBRARY_VERSION = "Library-Version";
    String LIBRARY_NAME = "Library-Name";
    */

}
