/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.osgi.service.blueprint.context;

public interface ModuleContextEventConstants  {
    
    static final String BUNDLE_VERSION = "bundle.version";
    
    static final String EXTENDER_BUNDLE = "extender.bundle";
    
    static final String EXTENDER_ID = "extender.bundle.id";
    
    static final String EXTENDER_SYMBLOICNAME = "extender.bundle.symbolicName";
    
    static final String TOPIC_BLUEPRINT_EVENTS = "org/osgi/service/blueprint";
    
    static final String TOPIC_CREATED = TOPIC_BLUEPRINT_EVENTS + "/context/CREATED";
    
    static final String TOPIC_CREATING = TOPIC_BLUEPRINT_EVENTS + "/context/CREATING";
    
    static final String TOPIC_DESTROYED = TOPIC_BLUEPRINT_EVENTS + "/context/DESTROYED";
    
    static final String TOPIC_DESTROYING = TOPIC_BLUEPRINT_EVENTS + "/context/DESTROYING";
    
    static final String TOPIC_FAILURE = TOPIC_BLUEPRINT_EVENTS + "/context/FAILURE";
    
    static final String TOPIC_WAITING = TOPIC_BLUEPRINT_EVENTS + "/context/WAITING";
      
}
