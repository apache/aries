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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
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
        
        boolean allowReorder = options.contains(Option.ARGUMENT_REORDER);

        // look for matching method using the order of arguments specified
        for (Method method : methods) {
            if (method.getName().equals(name) && isAcceptable(method, options)) {
                Class[] parameterTypes = method.getParameterTypes();
                if (isAssignable(parameterTypes, arguments, false, true)) {
                    matches.add(new ArgumentsMatch(method, arguments));
                }
            }
        }

        if (matches.size() == 0) {
            for (Method method : methods) {
                if (method.getName().equals(name) && isAcceptable(method, options)) {
                    Class[] parameterTypes = method.getParameterTypes();
                    if (isAssignable(parameterTypes, arguments, false, false)) {
                        matches.add(new ArgumentsMatch(method, arguments));
                    }
                }
            }
        }

        if (matches.size() == 0) {
            for (Method method : methods) {
                if (method.getName().equals(name) && isAcceptable(method, options)) {
                    Class[] parameterTypes = method.getParameterTypes();
                    if (isAssignable(parameterTypes, arguments, true, false)) {
                        matches.add(new ArgumentsMatch(method, arguments));
                    }
                }
            }
        }

        if (matches.size() == 0 && arguments.size() > 1 && allowReorder) {
            // we did not find any matching method, let's try re-ordering the arguments
            for (Method method : methods) {
                if (method.getName().equals(name) && isAcceptable(method, options)) {
                    Class[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == arguments.size()) {
                        ArgumentMatcher matcher = new ArgumentMatcher(method, false, true);
                        ArgumentsMatch match = matcher.match(arguments);
                        if (match != null) {
                            matches.add(match);
                        }
                    }
                }
            }
        }

        if (matches.size() == 0 && arguments.size() > 1 && allowReorder) {
            // we did not find any matching method, let's try re-ordering the arguments
            for (Method method : methods) {
                if (method.getName().equals(name) && isAcceptable(method, options)) {
                    Class[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == arguments.size()) {
                        ArgumentMatcher matcher = new ArgumentMatcher(method, false, false);
                        ArgumentsMatch match = matcher.match(arguments);
                        if (match != null) {
                            matches.add(match);
                        }
                    }
                }
            }
        }

        if (matches.size() == 0 && arguments.size() > 1 && allowReorder) {
            // we did not find any matching method, let's try re-ordering the arguments
            for (Method method : methods) {
                if (method.getName().equals(name) && isAcceptable(method, options)) {
                    Class[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == arguments.size()) {
                        ArgumentMatcher matcher = new ArgumentMatcher(method, true, false);
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
        Set<ArgumentsMatch> matches = new HashSet<ArgumentsMatch>();
        Constructor[] constructors = type.getConstructors();
        
        boolean allowReorder = options.contains(Option.ARGUMENT_REORDER);

        // look for matching constructor using the exact match
        for (Constructor constructor : constructors) {
            Class[] parameterTypes = constructor.getParameterTypes();
            if (isAssignable(parameterTypes, arguments, false, true)) {
                matches.add(new ArgumentsMatch(constructor, arguments));
            }
        }

        if (matches.size() == 0) {
            for (Constructor constructor : constructors) {
                Class[] parameterTypes = constructor.getParameterTypes();
                if (isAssignable(parameterTypes, arguments, false, false)) {
                    matches.add(new ArgumentsMatch(constructor, arguments));
                }
            }
        }

        // look for matching constructor using the order of arguments specified in config file
        if (matches.size() == 0) {
            for (Constructor constructor : constructors) {
                Class[] parameterTypes = constructor.getParameterTypes();
                if (isAssignable(parameterTypes, arguments, true, false)) {
                    matches.add(new ArgumentsMatch(constructor, arguments));
                }
            }
        }

        if (matches.size() == 0 && arguments.size() > 1 && allowReorder) {
            // we did not find any matching constructor, let's try re-ordering the arguments
            for (Constructor constructor : constructors) {
                Class[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == arguments.size()) {
                    ArgumentMatcher matcher = new ArgumentMatcher(constructor, false, true);
                    ArgumentsMatch match = matcher.match(arguments);
                    if (match != null) {
                        matches.add(match);
                    }
                }
            }
        }

        if (matches.size() == 0 && arguments.size() > 1 && allowReorder) {
            // we did not find any matching constructor, let's try re-ordering the arguments
            for (Constructor constructor : constructors) {
                Class[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == arguments.size()) {
                    ArgumentMatcher matcher = new ArgumentMatcher(constructor, false, false);
                    ArgumentsMatch match = matcher.match(arguments);
                    if (match != null) {
                        matches.add(match);
                    }
                }
            }
        }

        if (matches.size() == 0 && arguments.size() > 1 && allowReorder) {
            // we did not find any matching constructor, let's try re-ordering the
            for (Constructor constructor : constructors) {
                Class[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == arguments.size()) {
                    ArgumentMatcher matcher = new ArgumentMatcher(constructor, true, false);
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
            return matches.iterator().next();
        } else {
            throw new RuntimeException("Found multiple matching constructors");
        }
    }
    
    public static boolean isAssignable(Class[] parameterTypes, List<Object> arguments, boolean allowConversion, boolean strict) {
        if (parameterTypes.length == arguments.size()) {
            boolean assignable = true;
            for (int i = 0; i < parameterTypes.length && assignable; i++) {
                assignable = isAssignable(parameterTypes[i], arguments.get(i), allowConversion, strict);
            }
            return assignable;
        }
        return false;
    }
    
    public static boolean isAssignable(Class type, Object argument, boolean allowConversion, boolean strict) {
        if (argument == null) {
            return true;
        } else if (argument instanceof Recipe) {
            Recipe recipe = (Recipe) argument;
            if (allowConversion) {
                // TODO: use conversion service
                return recipe.canCreate(type);
            } else {
                for (Type t : recipe.getTypes()) {
                    if (type.equals(RecipeHelper.toClass(t))) {
                        return true;
                    }
                }
                return false;
            }
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
        private boolean allowConversion;
        private boolean strict;
        
        public ArgumentMatcher(Constructor constructor, boolean allowConversion, boolean strict) {
            this.constructor = constructor;
            buildTypes(constructor.getParameterTypes());
            this.allowConversion = allowConversion;
            this.strict = strict;
        }
        
        public ArgumentMatcher(Method method, boolean allowConversion, boolean strict) {
            this.method = method;
            buildTypes(method.getParameterTypes());
            this.allowConversion = allowConversion;
            this.strict = strict;
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
                    ArgumentsMatcher.isAssignable(entry.getType(), arg, allowConversion, strict)) {
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
