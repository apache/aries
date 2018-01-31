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
package org.apache.aries.blueprint.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.aries.blueprint.di.CircularDependencyException;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.RefRecipe;
import org.osgi.service.blueprint.container.NoSuchComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyGraph.class);

    private BlueprintRepository repository;
    
    public DependencyGraph(BlueprintRepository repository) {
        this.repository = repository;        
    }
    
    public LinkedHashMap<String, Recipe> getSortedRecipes(Collection<String> names) {
        // construct the graph
        Map<String, Node> nodes = new LinkedHashMap<String, Node>();
        for (String name : names) {
            Object object = repository.getObject(name);
            if (object == null) {
                throw new NoSuchComponentException(name);
            }
            if (object instanceof Recipe) {
                Recipe recipe = (Recipe) object;
                if (!recipe.getName().equals(name)) {
                    throw new RuntimeException("Recipe '" + name + "' returned from the repository has name '" + name + "'");
                }
                createNode(name, recipe, nodes);
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
            throw new RuntimeException("Internal Error: expected a CircularDependencyException");
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
                throw new RuntimeException("The name '" + name + "' is assigned to multiple recipies");
            }
            return node;
        }

        // create the node
        Node node = new Node();
        node.name = name;
        node.recipe = recipe;
        nodes.put(name, node);

        // link in the references
        LinkedList<Recipe> constructorRecipes = new LinkedList<Recipe>(recipe.getConstructorDependencies());
        while (!constructorRecipes.isEmpty()) {
            Recipe nestedRecipe = constructorRecipes.removeFirst();            
            if (nestedRecipe instanceof RefRecipe) {
                nestedRecipe = nestedRecipe.getDependencies().get(0);
                String nestedName = nestedRecipe.getName();
                Node nestedNode = createNode(nestedName, nestedRecipe, nodes);
                node.referenceCount++;
                nestedNode.references.add(node);                
            } else {
                constructorRecipes.addAll(nestedRecipe.getDependencies());
            }
        }
        
        return node;
    }

    private class Node {
        String name;
        Recipe recipe;
        final List<Node> references = new ArrayList<Node>();
        int referenceCount;
        
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("Node[").append(name);
            if (references.size() > 0) {
                buf.append(" <- ");
                Iterator<Node> iter = references.iterator();
                while (iter.hasNext()) {
                    buf.append(iter.next().name);
                    if (iter.hasNext()) {
                        buf.append(", ");
                    }
                }
            }
            buf.append("]");
            return buf.toString();
        }

    }
}
