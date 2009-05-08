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
package org.apache.geronimo.blueprint.context;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.DefaultExecutionContext;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.xbean.recipe.NoSuchObjectException;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.Repository;

/**
 */
public class BlueprintObjectInstantiator  {

    private Repository repository;

    public BlueprintObjectInstantiator(Repository repository) {
        this.repository = repository;
    }
    
    public  Repository getRepository() {
        return repository;
    }
    
    public Object create(String name) throws ConstructionException {
        Map<String, Object> instances = createAll(Arrays.asList(name));
        return instances.get(name);
    }
    
    public Map<String,Object> createAll(String... names) throws ConstructionException {
        return createAll(Arrays.asList(names));
    }
        
    public Map<String, Object> createAll(Collection<String> names) throws ConstructionException {
        Map<String, Object> instances = new LinkedHashMap<String, Object>();
        for (String name : names) {
            
            boolean createNewContext = !ExecutionContext.isContextSet();
            if (createNewContext) {
                ExecutionContext.setContext(new DefaultExecutionContext(repository));
            }
            
            try {
                Object obj = createInstance(name);
                instances.put(name, obj);
            } finally {
                if (createNewContext) {
                    ExecutionContext.setContext(null);
                }
            }
        }
        return instances;
    }
    
    private Object createInstance(String name) {
        Object recipe = repository.get(name);
        if (recipe == null) {
            throw new NoSuchObjectException(name);
        }
        Object obj = recipe;
        if (recipe instanceof Recipe) {
            obj = ((Recipe) recipe).create(Object.class, false);
        }
        return obj;
    }
        
}
