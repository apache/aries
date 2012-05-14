/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.di;

import java.util.Collections;
import java.util.List;

import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.NoSuchComponentException;

public class RefRecipe extends AbstractRecipe {

    private String idRef;

    public RefRecipe(String name, String idRef) {
        super(name);
        this.idRef = idRef;
    }

    public String getIdRef() {
        return idRef;
    }

    public void setIdRef(String name) {
        this.idRef = name;
    }

    public List<Recipe> getDependencies() {
        Recipe recipe = ExecutionContext.Holder.getContext().getRecipe(idRef);
        if (recipe != null) {
            return Collections.singletonList(recipe);
        } else {
            return Collections.emptyList();
        }
    }

    protected Object internalCreate() throws ComponentDefinitionException {
        ExecutionContext context = ExecutionContext.Holder.getContext();
        if (!context.containsObject(idRef)) {
            throw new NoSuchComponentException(idRef);
        }
        Object instance = context.getObject(idRef);
        if (instance instanceof Recipe) {
            Recipe recipe = (Recipe) instance;
            instance = recipe.create();
        }
        return instance;
    }

    @Override
    public String toString() {
        return "RefRecipe[" +
                "name='" + name + '\'' +
                ", idRef='" + idRef + '\'' +
                ']';
    }

}
