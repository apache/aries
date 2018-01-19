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
package org.apache.aries.blueprint;

import java.util.Collection;

import org.osgi.service.blueprint.reflect.ReferenceMetadata;

public interface ExtendedReferenceMetadata extends ReferenceMetadata 
{
    public int DAMPING_RELUCTANT = 0;

    public int DAMPING_GREEDY = 1;

    public int LIFECYCLE_DYNAMIC = 0;

    public int LIFECYCLE_STATIC = 1;

    public String getDefaultBean();
    
    public Collection<Class<?>> getProxyChildBeanClasses();
    
    public Collection<String> getExtraInterfaces();

    public int getDamping();

    public int getLifecycle();

}