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
package org.apache.xbean.recipe;

import java.util.Map;
import java.util.Properties;
import java.lang.reflect.Type;

public class AllPropertiesRecipe extends AbstractRecipe {
    public boolean canCreate(Type type) {
        return RecipeHelper.isAssignable(type, Properties.class);
    }

    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        Recipe outerRecipe = RecipeHelper.getCaller();
        if (!(outerRecipe instanceof ObjectRecipe)) {
            throw new ConstructionException("UnsetPropertiesRecipe can only be nested in an ObjectRecipe: outerRecipe=" + outerRecipe);
        }
        ObjectRecipe objectRecipe = (ObjectRecipe) outerRecipe;
        Map<String,Object> allProperties = objectRecipe.getProperties();

        // copy to a properties object
        Properties properties = new Properties();
        for (Map.Entry<String, Object> entry : allProperties.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }

        // add to execution context if name is specified
        if (getName() != null) {
            ExecutionContext.getContext().addObject(getName(), properties);
        }
        
        return properties;
    }
}