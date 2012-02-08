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
package org.apache.felix.resolver;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.Wire;
import org.osgi.service.resolver.Resolver;

public interface FelixResolver extends Resolver
{
    Map<Resource, List<Wire>> resolve(
        FelixEnvironment env,
        Collection<? extends Resource> mandatoryRevisions,
        Collection<? extends Resource> optionalRevisions,
        Collection<? extends Resource> ondemandFragments);
    Map<Resource, List<Wire>> resolve(
        FelixEnvironment env,
        Resource resource,
        Requirement dynReq,
        SortedSet<Capability> cands,
        Collection<? extends Resource> ondemandFragments);
}