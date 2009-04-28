/**
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
package org.apache.geronimo.blueprint.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.RecipeHelper;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class ArgumentsMatcher {
           
    public enum Option {
        ARGUMENT_REORDER,
        STATIC_METHODS_ONLY,
        INSTANCE_METHODS_ONLY
    }
    
    public static ArgumentsMatch findMethod(Class type, String name, List<Object> arguments, Set<Option> options) { 
        List<ArgumentsMatch> matches = new ArrayList<ArgumentsMatch>();
        Method[] methods = type.getMethods();
        
        // look for matching method using the order of arguments specified    
        for (Method method : methods) {
            if (method.getName().equals(name) && isAcceptable(method, options)) {
                Class[] parameterTypes = method.getParameterTypes();
                if (isAssignable(parameterTypes, arguments)) {
                    matches.add(new ArgumentsMatch(method, arguments));
                }
            }
        }
        
        boolean allowReorder = options.contains(Option.ARGUMENT_REORDER); 
        if (matches.size() == 0 && arguments.size() > 1 && allowReorder) {
            // we did not find any matching method, let's try re-ordering the arguments        
            for (Method method : methods) {
                if (method.getName().equals(name) && isAcceptable(method, options)) {
                    Class[] parameterTypes = method.getParameterTypes();                
                    if (parameterTypes.length == arguments.size()) {   
                        ArgumentMatcher matcher = new ArgumentMatcher(method);
                        ArgumentsMatch match = matcher.match(arguments);
                        if (match != null) {
                            matches.add(match);
                        }
                    }
                }
            }
        }
        
        int size = matches.size();
        if (size == 0) {
            throw new RuntimeException("Did not find any matching method");
        } else if (size == 1) {
            return matches.get(0);
        } else {
            throw new RuntimeException("Found multiple matching methods");
        }
    }
       
    public static ArgumentsMatch findConstructor(Class type, List<Object> arguments, Set<Option> options) { 
        List<ArgumentsMatch> matches = new ArrayList<ArgumentsMatch>();
        Constructor[] constructors = type.getConstructors();
        
        // look for matching constructor using the order of arguments specified in config file                        
        for (Constructor constructor : constructors) {
            Class[] parameterTypes = constructor.getParameterTypes();
            if (isAssignable(parameterTypes, arguments)) {
                matches.add(new ArgumentsMatch(constructor, arguments));
            }
        }
            
        boolean allowReorder = options.contains(Option.ARGUMENT_REORDER);
        if (matches.size() == 0 && arguments.size() > 1 && allowReorder) {
            // we did not find any matching constructor, let's try re-ordering the arguments            
            for (Constructor constructor : constructors) {
                Class[] parameterTypes = constructor.getParameterTypes();                  
                if (parameterTypes.length == arguments.size()) {   
                    ArgumentMatcher matcher = new ArgumentMatcher(constructor);
                    ArgumentsMatch match = matcher.match(arguments);
                    if (match != null) {
                        matches.add(match);
                    }                   
                }
            }
        }
        
        int size = matches.size();
        if (size == 0) {
            throw new RuntimeException("Did not find any matching constructor");
        } else if (size == 1) {
            return matches.get(0);
        } else {
            throw new RuntimeException("Found multiple matching constructors");
        }
    }
    
    public static boolean isAssignable(Class[] parameterTypes, List<Object> arguments) {
        if (parameterTypes.length == arguments.size()) {
            boolean assignable = true;
            for (int i = 0; i < parameterTypes.length && assignable; i++) {
                assignable = isAssignable(parameterTypes[i], arguments.get(i));
            }
            return assignable;
        }
        return false;
    }
    
    public static boolean isAssignable(Class type, Object argument) {
        if (argument == null) {
            return true;
        } else if (argument instanceof Recipe) {
            Recipe recipe = (Recipe) argument;
            return recipe.canCreate(type);
        } else {
            return RecipeHelper.isAssignableFrom(type, argument.getClass());
        }
    }
            
    private static boolean isAcceptable(Method method, Set<Option> options) {
        if (options.contains(Option.STATIC_METHODS_ONLY)) {
            return Modifier.isStatic(method.getModifiers());
        } else if (options.contains(Option.INSTANCE_METHODS_ONLY)) {
            return !Modifier.isStatic(method.getModifiers());
        } else {
            return true;
        }
    }
    
    private static class ArgumentMatcher {
        
        private List<TypeEntry> entries;
        private Method method;
        private Constructor constructor;
        
        public ArgumentMatcher(Constructor constructor) {
            this.constructor = constructor;
            buildTypes(constructor.getParameterTypes());
        }
        
        public ArgumentMatcher(Method method) {
            this.method = method;
            buildTypes(method.getParameterTypes());
        }
        
        private void buildTypes(Class[] types) {
            entries = new ArrayList<TypeEntry>();
            for (Class type : types) {
                entries.add(new TypeEntry(type));
            }
        }
            
        public Method getMethod() {
            return method;
        }
        
        public Constructor getConstructor() {
            return constructor;
        }
        
        public List<Object> getArguments() {
            List<Object> list = new ArrayList<Object>();
            for (TypeEntry entry : entries) {
                Object arg = entry.getArgument();
                if (arg == null) {
                    throw new RuntimeException("There are unmatched types");
                } else {
                    list.add(arg);
                }
            }
            return list;
        }
                
        public ArgumentsMatch match(List<Object> arguments) {
            if (find(arguments)) {
                List<Object> newList = getArguments();        
                if (constructor != null) {
                    return new ArgumentsMatch(constructor, newList);
                } else if (method != null) {
                    return new ArgumentsMatch(method, newList);                
                }
            }
            return null;
        }
        
        public boolean find(List<Object> arguments) {
            if (entries.size() == arguments.size()) {
                boolean matched = true;
                for (int i = 0; i < arguments.size() && matched; i++) {
                    matched = find(arguments.get(i));
                }
                return matched;
            }
            return false;
        }
        
        private boolean find(Object arg) {
            for (TypeEntry entry : entries) {
                if (entry.getArgument() == null &&
                    ArgumentsMatcher.isAssignable(entry.getType(), arg)) {
                    entry.setArgument(arg);
                    return true;
                }
            }
            return false;
        }
            
        public boolean isMatched() {
            for (TypeEntry entry : entries) {
                if (entry.getArgument() == null) {
                    return false;
                }
            }
            return true;
        }
    }
         
    private static class TypeEntry {
        
        private Class type;
        private Object argument;
        
        public TypeEntry(Class type) {
            this.type = type;
        }
        
        public Class getType() {
            return type;
        }
        
        public void setArgument(Object arg) {
            this.argument = arg;
        }
        
        public Object getArgument() {
            return argument;
        }

    }
        
}
