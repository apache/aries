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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

public class ObjectGraph {
    private Repository repository;

    public ObjectGraph() {
        this(new DefaultRepository());
    }

    public ObjectGraph(Repository repository) {
        if (repository == null) throw new NullPointerException("repository is null");
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        if (repository == null) throw new NullPointerException("repository is null");
        this.repository = repository;
    }

    public Object create(String name) throws ConstructionException {
        Map<String, Object> objects = createAll(Collections.singletonList(name));
        Object instance = objects.get(name);
        if (instance == null) {
            instance = repository.get(name);
        }
        return instance;
    }

    public Map<String,Object> createAll(String... names) throws ConstructionException {
        return createAll(Arrays.asList(names));
    }

    public Map<String,Object> createAll(List<String> names) throws ConstructionException {
        // setup execution context
        boolean createNewContext = !ExecutionContext.isContextSet();
        if (createNewContext) {
            ExecutionContext.setContext(new DefaultExecutionContext(repository));
        }
        WrapperExecutionContext wrapperContext = new WrapperExecutionContext(ExecutionContext.getContext());
        ExecutionContext.setContext(wrapperContext);

        try {
            // find recipes to create
            LinkedHashMap<String, Recipe> recipes = getSortedRecipes(names);

            // Seed the objects linked hash map with the existing objects
            LinkedHashMap<String, Object> objects = new LinkedHashMap<String, Object>();
            List<String> existingObjectNames = new ArrayList<String>(names);
            existingObjectNames.removeAll(recipes.keySet());
            for (String name : existingObjectNames) {
                Object object = repository.get(name);
                if (object == null) {
                    throw new NoSuchObjectException(name);
                }
                objects.put(name, object);
            }

            // build each object from the recipe
            for (Map.Entry<String, Recipe> entry : recipes.entrySet()) {
                String name = entry.getKey();
                Recipe recipe = entry.getValue();
                if (!wrapperContext.containsObject(name) || wrapperContext.getObject(name) instanceof Recipe) {
                    recipe.create(Object.class, false);
                }
            }

            // add the constructed objects to the objects linked hash map
            // The result map will be in construction order, with existing
            // objects at the front
            objects.putAll(wrapperContext.getConstructedObject());
            return objects;
        } finally {
            // if we set a new execution context, remove it from the thread
            if (createNewContext) {
                ExecutionContext.setContext(null);
            }
        }
    }

    private LinkedHashMap<String, Recipe> getSortedRecipes(List<String> names) {
        // construct the graph
        Map<String, Node> nodes = new LinkedHashMap<String, Node>();
        for (String name : names) {
            Object object = repository.get(name);
            if (object instanceof Recipe) {
                Recipe recipe = (Recipe) object;
                if (!recipe.getName().equals(name)) {
                    throw new ConstructionException("Recipe '" + name + "' returned from the repository has name '" + name + "'");
                }
                createNode(name, recipe,  nodes);
            }
        }

        // find all initial leaf nodes (and islands)
        List<Node> sortedNodes = new ArrayList<Node>(nodes.size());
        LinkedList<Node> leafNodes = new LinkedList<Node>();
        for (Node n : nodes.values()) {
            if (n.referenceCount == 0) {
                // if the node is totally isolated (no in or out refs),
                // move it directly to the finished list, so they are first
                if (n.references.size() == 0) {
                    sortedNodes.add(n);
                } else {
                    leafNodes.add(n);
                }
            }
        }

        // pluck the leaves until there are no leaves remaining
        while (!leafNodes.isEmpty()) {
            Node node = leafNodes.removeFirst();
            sortedNodes.add(node);
            for (Node ref : node.references) {
                ref.referenceCount--;
                if (ref.referenceCount == 0) {
                    leafNodes.add(ref);
                }
            }
        }

        // There are no more leaves so if there are there still
        // unprocessed nodes in the graph, we have one or more curcuits
        if (sortedNodes.size() != nodes.size()) {
            findCircuit(nodes.values().iterator().next(), new ArrayList<Recipe>(nodes.size()));
            // find circuit should never fail, if it does there is a programming error
            throw new ConstructionException("Internal Error: expected a CircularDependencyException");
        }

        // return the recipes
        LinkedHashMap<String, Recipe> sortedRecipes = new LinkedHashMap<String, Recipe>();
        for (Node node : sortedNodes) {
            sortedRecipes.put(node.name, node.recipe);
        }
        return sortedRecipes;
    }

    private void findCircuit(Node node, ArrayList<Recipe> stack) {
        if (stack.contains(node.recipe)) {
            ArrayList<Recipe> circularity = new ArrayList<Recipe>(stack.subList(stack.indexOf(node.recipe), stack.size()));

            // remove anonymous nodes from circularity list
            for (Iterator<Recipe> iterator = circularity.iterator(); iterator.hasNext();) {
                Recipe recipe = iterator.next();
                if (recipe != node.recipe && recipe.getName() == null) {
                    iterator.remove();
                }
            }

            // add ending node to list so a full circuit is shown
            circularity.add(node.recipe);
            
            throw new CircularDependencyException(circularity);
        }

        stack.add(node.recipe);
        for (Node reference : node.references) {
            findCircuit(reference, stack);
        }
    }

    private Node createNode(String name, Recipe recipe, Map<String, Node> nodes) {
        // if node already exists, verify that the exact same recipe instnace is used for both
        if (nodes.containsKey(name)) {
            Node node = nodes.get(name);
            if (node.recipe != recipe) {
                throw new ConstructionException("The name '" + name +"' is assigned to multiple recipies");
            }
            return node;
        }

        // create the node
        Node node = new Node();
        node.name = name;
        node.recipe = recipe;
        nodes.put(name, node);

        // link in the references
        LinkedList<Recipe> nestedRecipes = new LinkedList<Recipe>(recipe.getNestedRecipes());
        LinkedList<Recipe> constructorRecipes = new LinkedList<Recipe>(recipe.getConstructorRecipes());
        while (!nestedRecipes.isEmpty()) {
            Recipe nestedRecipe = nestedRecipes.removeFirst();
            String nestedName = nestedRecipe.getName();
            if (nestedName != null) {
                Node nestedNode = createNode(nestedName, nestedRecipe, nodes);

                // if this is a constructor recipe, we need to add a reference link
                if (constructorRecipes.contains(nestedRecipe)) {
                    node.referenceCount++;
                    nestedNode.references.add(node);
                }
            } else {
                nestedRecipes.addAll(nestedRecipe.getNestedRecipes());
                constructorRecipes.addAll(nestedRecipe.getConstructorRecipes());
            }
        }

        return node;
    }

    private class Node {
        String name;
        Recipe recipe;
        final List<Node> references = new ArrayList<Node>();
        int referenceCount;
    }

    private static class WrapperExecutionContext extends ExecutionContext {
        private final ExecutionContext executionContext;
        private final Map<String, Object> constructedObject = new LinkedHashMap<String, Object>();

        private WrapperExecutionContext(ExecutionContext executionContext) {
            if (executionContext == null) throw new NullPointerException("executionContext is null");
            this.executionContext = executionContext;
        }

        public Map<String, Object> getConstructedObject() {
            return constructedObject;
        }

        public void push(Recipe recipe) throws CircularDependencyException {
            executionContext.push(recipe);
        }

        public Recipe pop() {
            return executionContext.pop();
        }

        public LinkedList<Recipe> getStack() {
            return executionContext.getStack();
        }

        public Object getObject(String name) {
            return executionContext.getObject(name);
        }

        public boolean containsObject(String name) {
            return executionContext.containsObject(name);
        }

        public void addObject(String name, Object object) {
            executionContext.addObject(name, object);
            constructedObject.put(name, object);
        }

        public void addReference(Reference reference) {
            executionContext.addReference(reference);
        }

        public Map<String, List<Reference>> getUnresolvedRefs() {
            return executionContext.getUnresolvedRefs();
        }

        public ClassLoader getClassLoader() {
            return executionContext.getClassLoader();
        }
    }
}
