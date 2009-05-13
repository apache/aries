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

import java.util.*;

public class DefaultExecutionContext extends ExecutionContext {
    /**
     * The source of recipes and existing objects.
     */
    private Repository repository;

    /**
     * Before each recipe is executed it is pushed on the stack.  The
     * stack is used to detect circular dependencies and so a recipe can
     * access the caller recipe (e.g. UnsetPropertiesRecipe returns a
     * map of the caller's unset properties)
     */
    private final LinkedList<Recipe> stack = new LinkedList<Recipe>();

    /**
     * The unresolved references by name.
     */
    private final SortedMap<String, List<Reference>> unresolvedRefs = new TreeMap<String, List<Reference>>();

    public DefaultExecutionContext() {
        this(new DefaultRepository());
    }

    public DefaultExecutionContext(Repository repository) {
        if (repository == null) throw new NullPointerException("repository is null");
        this.repository = repository;
    }

    public void push(Recipe recipe) {
        if (stack.contains(recipe)) {
            ArrayList<Recipe> circularity = new ArrayList<Recipe>(stack.subList(stack.indexOf(recipe), stack.size()));

            // remove anonymous nodes from circularity list
            for (Iterator<Recipe> iterator = circularity.iterator(); iterator.hasNext();) {
                Recipe item = iterator.next();
                if (item != recipe && item.getName() == null) {
                    iterator.remove();
                }
            }

            // add ending node to list so a full circuit is shown
            circularity.add(recipe);

            throw new CircularDependencyException(circularity);
        }
        stack.add(recipe);
    }

    public Recipe pop() {
        return stack.removeLast();
    }

    public LinkedList<Recipe> getStack() {
        return new LinkedList<Recipe>(stack);
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        if (repository == null) throw new NullPointerException("repository is null");
        this.repository = repository;
    }

    public boolean containsObject(String name) {
        boolean contains = repository.contains(name);
        return contains;
    }

    public Object getObject(String name) {
        Object object = repository.get(name);
        return object;
    }

    public void addObject(String name, Object object) {
        repository.add(name, object);

        // set any pending references
        List<Reference> list = unresolvedRefs.remove(name);
        if (list != null) {
            for (Reference Reference : list) {
                Reference.set(object);
            }
        }
    }

    public void addReference(Reference reference) {
        Object value = repository.get(reference.getName());
        if (value != null && !(value instanceof Recipe)) {
            reference.set(value);
        } else {
            List<Reference> list = unresolvedRefs.get(reference.getName());
            if (list == null) {
                list = new ArrayList<Reference>();
                unresolvedRefs.put(reference.getName(), list);
            }
            list.add(reference);
        }
    }

    public SortedMap<String, List<Reference>> getUnresolvedRefs() {
        return unresolvedRefs;
    }

    public ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) classLoader = getClass().getClassLoader();
        return classLoader;
    }
}
