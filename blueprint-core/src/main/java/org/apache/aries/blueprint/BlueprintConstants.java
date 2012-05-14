/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint;

public interface BlueprintConstants  {
   
    public static final String BUNDLE_BLUEPRINT_HEADER = "Bundle-Blueprint";
    
    public static final String BUNDLE_BLUEPRINT_ANNOTATION_HEADER = "Bundle-Blueprint-Annotation";
    
    public static final String TIMEOUT_DIRECTIVE = "blueprint.timeout";
    
    public static final String GRACE_PERIOD = "blueprint.graceperiod";
    
    public static final String BUNDLE_VERSION = "bundle.version";
    
    public static final String COMPONENT_NAME_PROPERTY = "osgi.service.blueprint.compname";
    
    public static final String CONTAINER_SYMBOLIC_NAME_PROPERTY = "osgi.blueprint.container.symbolicname";
    
    public static final String CONTAINER_VERSION_PROPERTY = "osgi.blueprint.container.version";

    public static final String XML_VALIDATION = "blueprint.aries.xml-validation";
}
