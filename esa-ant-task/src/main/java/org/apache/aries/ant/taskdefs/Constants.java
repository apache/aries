/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.ant.taskdefs;

/**
 * 
 * @version $Id: $
 */

public interface Constants {
	
	public static final String BUNDLE_VERSION = "Bundle-Version";
    public static final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";
    public static final String FRAGMENT_HOST = "Fragment-Host";
    
    public static final String BUNDLE_TYPE = "osgi.bundle";
    public static final String FRAGMENT_TYPE = "osgi.fragment";
    
    /*
     * Subsystem types
     */
    public static final String APPLICATION_TYPE = "osgi.subsystem.application";
    public static final String COMPOSITE_TYPE = "osgi.subsystem.composite";
    public static final String FEATURE_TYPE = "osgi.subsystem.feature";

    /*
     * Subsystem manifest headers
     */
    public static final String SUBSYSTEM_MANIFESTVERSION = "Subsystem-ManifestVersion";
    public static final String SUBSYSTEM_SYMBOLICNAME = "Subsystem-SymbolicName";
    public static final String SUBSYSTEM_VERSION = "Subsystem-Version";
    public static final String SUBSYSTEM_NAME = "Subsystem-Name";
    public static final String SUBSYSTEM_DESCRIPTION = "Subsystem-Description";
    public static final String SUBSYSTEM_CONTENT = "Subsystem-Content";
    public static final String SUBSYSTEM_USEBUNDLE = "Use-Bundle";
    public static final String SUBSYSTEM_TYPE = "Subsystem-Type";
    
    public static final String OSGI_INF_PATH = "OSGI-INF/";
    public static final String SUBSYSTEM_MANIFEST_NAME = "OSGI-INF/SUBSYSTEM.MF";
    public static final String SUBSYSTEM_MANIFEST_VERSION_VALUE = "1";
}
