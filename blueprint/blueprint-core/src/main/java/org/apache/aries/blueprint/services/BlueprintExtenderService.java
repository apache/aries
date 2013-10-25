/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.blueprint.services;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintContainer;

public interface BlueprintExtenderService {

    /**
     * Create Blueprint container for the application bundle 
     * @param bundle the application bundle
     * @return container
     */
    BlueprintContainer createContainer(Bundle bundle);

    /**
     * Create Blueprint container for the application bundle using a list of Blueprint resources 
     * @param bundle the application bundle
     * @param blueprintPaths the application blueprint resources
     * @return container
     */    
    BlueprintContainer createContainer(Bundle bundle, List<Object> blueprintPaths);

    /**
     * Get an existing container for the application bundle
     * @param bundle the application bundle
     * @return container
     */
    BlueprintContainer getContainer(Bundle bundle);

    /**
     * Destroy Blueprint container for the application bundle
     * @param bundle the application bundle
     * @param container the container
     */
    void destroyContainer(Bundle bundle, BlueprintContainer container);
}

