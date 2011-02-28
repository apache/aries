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
package org.apache.aries.blueprint.di;

import java.util.Collections;
import java.util.List;

import org.osgi.service.blueprint.container.ComponentDefinitionException;

/**
 * @version $Rev$ $Date$
 */
public class PassThroughRecipe extends AbstractRecipe {

    private Object object;

    public PassThroughRecipe(String id, Object object) {
        super(id);
        this.prototype = false;
        this.object = object;
    }

    protected Object internalCreate() throws ComponentDefinitionException {
        return object;
    }

    public List<Recipe> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "EnvironmentRecipe[" +
                "name='" + name + '\'' +
                ", object=" + object +
                ']';
    }
}
