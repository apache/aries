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
package org.osgi.service.blueprint.container;

import java.util.Collection;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

public interface BlueprintContainer {
    
    static final int CONFIGURATION_ADMIN_OBJECT_DELETED = 1;
   
    static final int BUNDLE_STOPPING = 2;

    Set<String> getComponentIds();

    Object getComponentInstance(String id) throws NoSuchComponentException;

    ComponentMetadata getComponentMetadata(String id) throws NoSuchComponentException;

    <T extends ComponentMetadata> Collection<T> getMetadata(Class<T> type);

    BundleContext getBundleContext();

}
