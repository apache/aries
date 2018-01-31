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
   
    String BUNDLE_BLUEPRINT_HEADER = "Bundle-Blueprint";
    
    String BUNDLE_BLUEPRINT_ANNOTATION_HEADER = "Bundle-Blueprint-Annotation";
    
    String TIMEOUT_DIRECTIVE = "blueprint.timeout";
    
    String GRACE_PERIOD = "blueprint.graceperiod";
    
    String BUNDLE_VERSION = "bundle.version";
    
    String COMPONENT_NAME_PROPERTY = "osgi.service.blueprint.compname";
    
    String CONTAINER_SYMBOLIC_NAME_PROPERTY = "osgi.blueprint.container.symbolicname";
    
    String CONTAINER_VERSION_PROPERTY = "osgi.blueprint.container.version";

    String XML_VALIDATION = "blueprint.aries.xml-validation";

    String USE_SYSTEM_CONTEXT_PROPERTY = "org.apache.aries.blueprint.use.system.context";

    String IGNORE_UNKNOWN_NAMESPACE_HANDLERS_PROPERTY = "org.apache.aries.blueprint.parser.service.ignore.unknown.namespace.handlers";

    String PREEMPTIVE_SHUTDOWN_PROPERTY = "org.apache.aries.blueprint.preemptiveShutdown";

    String SYNCHRONOUS_PROPERTY = "org.apache.aries.blueprint.synchronous";

    String XML_VALIDATION_PROPERTY = "org.apache.aries.blueprint.xml.validation";

}
