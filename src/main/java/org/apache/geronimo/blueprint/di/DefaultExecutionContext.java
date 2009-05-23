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
package org.apache.geronimo.blueprint.di;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.utils.ConversionUtils;

public class DefaultExecutionContext extends ExecutionContext {
    /**
     * The source of recipes and existing objects.
     */
    private Repository repository;

    private ExtendedBlueprintContainer blueprintContainer;

    /**
     * Contains partial objects.
     */
    private Map<String, Object> createdObjects = new HashMap<String, Object>();

    private List<Recipe> createdRecipes = new ArrayList<Recipe>();
    
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

    public DefaultExecutionContext(ExtendedBlueprintContainer blueprintContainer) {
        this(blueprintContainer, new DefaultRepository());
    }

    public DefaultExecutionContext(ExtendedBlueprintContainer blueprintContainer, Repository repository) {
        if (blueprintContainer == null) throw new NullPointerException("blueprintContainer is null");
        if (repository == null) throw new NullPointerException("repository is null");
        this.blueprintContainer = blueprintContainer;
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
        createdRecipes.add(recipe);
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

    public void addObject(String name, Object object, boolean partialObject) {
        createdObjects.put(name, object);
        if (!partialObject) {
            addObject(name, object);
        }
    }
    
    public boolean containsCreatedObject(String name) {
        if (repository.contains(name)) {
            Object obj = repository.get(name);
            if (!(obj instanceof Recipe)) {
                return true;
            }
        }
        return createdObjects.containsKey(name);
    }
    
    public Object getCreatedObject(String name) {
        if (repository.contains(name)) {
            Object obj = repository.get(name);
            if (!(obj instanceof Recipe)) {
                return obj;
            }
        }
        return createdObjects.get(name);
    }

    public List<Recipe> getCreatedRecipes() {
        return createdRecipes;
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

    public Object convert(Object value, Type type) throws Exception {
        return ConversionUtils.convert(value, type, blueprintContainer.getConverter());
    }

    private static Map<String, Class> primitiveClasses = new HashMap<String, Class>();

    static {
        primitiveClasses.put("int", int.class);
        primitiveClasses.put("short", short.class);
        primitiveClasses.put("long", long.class);
        primitiveClasses.put("byte", byte.class);
        primitiveClasses.put("char", char.class);
        primitiveClasses.put("float", float.class);
        primitiveClasses.put("double", double.class);
        primitiveClasses.put("boolean", boolean.class);
    }

    public Class loadClass(String typeName) throws ClassNotFoundException {
        if (typeName == null) {
            return null;
        }
        Class clazz = primitiveClasses.get(typeName);
        if (clazz == null && typeName.startsWith("java.")) {
            // We can bypass classes starting with "java." because they are always delegated
            // to the system bundle, so we'll end up with the same class in all cases 
            clazz = getClass().getClassLoader().loadClass(typeName);
        }
        if (clazz == null) {
            clazz = blueprintContainer.loadClass(typeName);
        }
        return clazz;
    }
}
