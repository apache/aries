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
package org.apache.felix.blueprint.reflect;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ComponentMetadataImpl implements ComponentMetadata {

    private String name;
    private Set<String> explicitDependencies;
    
    protected ComponentMetadataImpl() {
    }
    
    protected ComponentMetadataImpl(ComponentMetadata source) {
        name = source.getName();
        explicitDependencies = new HashSet<String>(source.getExplicitDependencies());
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getExplicitDependencies() {
        if (explicitDependencies == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(explicitDependencies);
    }

    public void setExplicitDependencies(Set<String> explicitDependencies) {
        this.explicitDependencies = explicitDependencies;
    }
}
