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
package org.osgi.service.blueprint.reflect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.List;

public interface ServiceMetadata extends ComponentMetadata, Target {

    static final int AUTO_EXPORT_DISABLED = 1;
    
    static final int AUTO_EXPORT_INTERFACES = 2;

    static final int AUTO_EXPORT_CLASS_HIERARCHY = 3;

    static final int AUTO_EXPORT_ALL_CLASSES = 4;

    Target getServiceComponent();

    List<String> getInterfaceNames();

    int getAutoExportMode();

    List<MapEntry> getServiceProperties();

    int getRanking();
    
    Collection<RegistrationListener> getRegistrationListeners();

    List<String> getExplicitDependencies();
    
}
