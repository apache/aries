/*
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
package org.osgi.service.blueprint.context;

/**
 * TODO: javadoc
 */
public interface ModuleContextEventConstants {

    String BUNDLE_VERSION = "bundle.version";
    String EXTENDER_BUNDLE = "extender.bundle";
    String EXTENDER_ID = "extender.bundle.id";
    String EXTENDER_SYMBOLICNAME = "extender.bundle.symbolicName";
    String TOPIC_BLUEPRINT_EVENTS = "org/osgi/service/blueprint";
    String TOPIC_CREATING = TOPIC_BLUEPRINT_EVENTS + "/context/CREATING";
    String TOPIC_CREATED = TOPIC_BLUEPRINT_EVENTS + "/context/CREATED";
    String TOPIC_DESTROYED = TOPIC_BLUEPRINT_EVENTS + "/context/DESTROYED";
    String TOPIC_DESTROYING = TOPIC_BLUEPRINT_EVENTS + "/context/DESTROYING";
    String TOPIC_WAITING = TOPIC_BLUEPRINT_EVENTS + "/context/WAITING";
    String TOPIC_FAILURE = TOPIC_BLUEPRINT_EVENTS + "/context/FAILURE";


}
