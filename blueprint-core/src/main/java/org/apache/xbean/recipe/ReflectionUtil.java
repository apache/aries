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

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.apache.xbean.recipe.RecipeHelper.isAssignableFrom;

public final class ReflectionUtil {
    private static ParameterNameLoader parameterNamesLoader;
    static {
        try {
            Class<? extends ParameterNameLoader> loaderClass = ReflectionUtil.class.getClassLoader().loadClass("org.apache.xbean.recipe.AsmParameterNameLoader").asSubclass(ParameterNameLoader.class);
            parameterNamesLoader = loaderClass.newInstance();
        } catch (Throwable ignored) {
        }
    }

    private ReflectionUtil() {
    }

    public static Field findField(Class typeClass, String propertyName, Object propertyValue, Set<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (propertyName == null) throw new NullPointerException("name is null");
        if (propertyName.length() == 0) throw new IllegalArgumentException("name is an empty string");
        if (options == null) options = EnumSet.noneOf(Option.class);

        int matchLevel = 0;
        MissingAccessorException missException = null;

        if (propertyName.contains("/")){
            String[] strings = propertyName.split("/");
            if (strings == null || strings.length != 2) throw new IllegalArgumentException("badly formed <class>/<attribute> property name: " + propertyName);

            String className = strings[0];
            propertyName = strings[1];

            boolean found = false;
            while(!typeClass.equals(Object.class) && !found){
                if (typeClass.getName().equals(className)){
                    found = true;
                    break;
                } else {
                    typeClass = typeClass.getSuperclass();
                }
            }

            if (!found) throw new MissingAccessorException("Type not assignable to class: " + className, -1);
        }

        List<Field> fields = new ArrayList<Field>(Arrays.asList(typeClass.getDeclaredFields()));
        Class parent = typeClass.getSuperclass();
        while (parent != null){
            fields.addAll(Arrays.asList(parent.getDeclaredFields()));
            parent = parent.getSuperclass();
        }

        boolean allowPrivate = options.contains(Option.PRIVATE_PROPERTIES);
        boolean allowStatic = options.contains(Option.STATIC_PROPERTIES);
        boolean caseInsesnitive = options.contains(Option.CASE_INSENSITIVE_PROPERTIES);

        for (Field field : fields) {
            if (field.getName().equals(propertyName) || (caseInsesnitive && field.getName().equalsIgnoreCase(propertyName))) {

                if (!allowPrivate && !Modifier.isPublic(field.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Field is not public: " + field, matchLevel);
                    }
                    continue;
                }

                if (!allowStatic && Modifier.isStatic(field.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Field is static: " + field, matchLevel);
                    }
                    continue;
                }

                Class fieldType = field.getType();
                if (fieldType.isPrimitive() && propertyValue == null) {
                    if (matchLevel < 6) {
                        matchLevel = 6;
                        missException = new MissingAccessorException("Null can not be assigned to " +
                                fieldType.getName() + ": " + field, matchLevel);
                    }
                    continue;
                }


                if (!RecipeHelper.isInstance(fieldType, propertyValue) && !RecipeHelper.isConvertable(fieldType, propertyValue)) {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new MissingAccessorException((propertyValue == null ? "null" : propertyValue.getClass().getName()) + " can not be assigned or converted to " +
                                fieldType.getName() + ": " + field, matchLevel);
                    }
                    continue;
                }

                if (allowPrivate && !Modifier.isPublic(field.getModifiers())) {
                    setAccessible(field);
                }

                return field;
            }

        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid field: ");
            buffer.append("public ").append(" ").append(propertyValue == null ? "null" : propertyValue.getClass().getName());
            buffer.append(" ").append(propertyName).append(";");
            throw new MissingAccessorException(buffer.toString(), -1);
        }
    }

    public static Method findGetter(Class typeClass, String propertyName, Set<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (propertyName == null) throw new NullPointerException("name is null");
        if (propertyName.length() == 0) throw new IllegalArgumentException("name is an empty string");
        if (options == null) options = EnumSet.noneOf(Option.class);

        if (propertyName.contains("/")){
            String[] strings = propertyName.split("/");
            if (strings == null || strings.length != 2) throw new IllegalArgumentException("badly formed <class>/<attribute> property name: " + propertyName);

            String className = strings[0];
            propertyName = strings[1];

            boolean found = false;
            while(!typeClass.equals(Object.class) && !found){
                if (typeClass.getName().equals(className)){
                    found = true;
                    break;
                } else {
                    typeClass = typeClass.getSuperclass();
                }
            }

            if (!found) throw new MissingAccessorException("Type not assignable to class: " + className, -1);
        }

        String getterName = "get" + Character.toUpperCase(propertyName.charAt(0));
        if (propertyName.length() > 0) {
            getterName += propertyName.substring(1);
        }
        
        boolean allowPrivate = options.contains(Option.PRIVATE_PROPERTIES);
        boolean allowStatic = options.contains(Option.STATIC_PROPERTIES);
        boolean caseInsesnitive = options.contains(Option.CASE_INSENSITIVE_PROPERTIES);

        List<Method> methods = new ArrayList<Method>(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        for (Method method : methods) {
            if (method.getName().equals(getterName) || (caseInsesnitive && method.getName().equalsIgnoreCase(getterName))) {
                if (method.getParameterTypes().length > 0) {
                    continue;
                }
                if (method.getReturnType() == Void.TYPE) {
                    continue;
                }
                if (Modifier.isAbstract(method.getModifiers())) {
                    continue;
                }
                if (!allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                if (!allowStatic && Modifier.isStatic(method.getModifiers())) {
                    continue;
                }

                if (allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    setAccessible(method);
                }
                
                return method;
            }
        }
        
        return null;
    }
    
    public static Method findSetter(Class typeClass, String propertyName, Object propertyValue, Set<Option> options) {
        List<Method> setters = findAllSetters(typeClass, propertyName, propertyValue, options);
        return setters.get(0);
    }

    /**
     * Finds all valid setters for the property.  Due to automatic type conversion there may be more than one possible
     * setter that could be used to set the property.  The setters that do not require type converstion will be a the
     * head of the returned list of setters.
     * @param typeClass the class to search for setters
     * @param propertyName the name of the property
     * @param propertyValue the value that must be settable either directly or after conversion
     * @param options controls which setters are considered valid
     * @return the valid setters; never null or empty
     */
    public static List<Method> findAllSetters(Class typeClass, String propertyName, Object propertyValue, Set<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (propertyName == null) throw new NullPointerException("name is null");
        if (propertyName.length() == 0) throw new IllegalArgumentException("name is an empty string");
        if (options == null) options = EnumSet.noneOf(Option.class);

        if (propertyName.contains("/")){
            String[] strings = propertyName.split("/");
            if (strings == null || strings.length != 2) throw new IllegalArgumentException("badly formed <class>/<attribute> property name: " + propertyName);

            String className = strings[0];
            propertyName = strings[1];

            boolean found = false;
            while(!typeClass.equals(Object.class) && !found){
                if (typeClass.getName().equals(className)){
                    found = true;
                    break;
                } else {
                    typeClass = typeClass.getSuperclass();
                }
            }

            if (!found) throw new MissingAccessorException("Type not assignable to class: " + className, -1);
        }

        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0));
        if (propertyName.length() > 0) {
            setterName += propertyName.substring(1);
        }


        int matchLevel = 0;
        MissingAccessorException missException = null;

        boolean allowPrivate = options.contains(Option.PRIVATE_PROPERTIES);
        boolean allowStatic = options.contains(Option.STATIC_PROPERTIES);
        boolean caseInsesnitive = options.contains(Option.CASE_INSENSITIVE_PROPERTIES);


        LinkedList<Method> validSetters = new LinkedList<Method>();

        List<Method> methods = new ArrayList<Method>(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        for (Method method : methods) {
            if (method.getName().equals(setterName) || (caseInsesnitive && method.getName().equalsIgnoreCase(setterName))) {
                if (method.getParameterTypes().length == 0) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new MissingAccessorException("Setter takes no parameters: " + method, matchLevel);
                    }
                    continue;
                }

                if (method.getParameterTypes().length > 1) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new MissingAccessorException("Setter takes more then one parameter: " + method, matchLevel);
                    }
                    continue;
                }

                if (method.getReturnType() != Void.TYPE) {
                    if (matchLevel < 2) {
                        matchLevel = 2;
                        missException = new MissingAccessorException("Setter returns a value: " + method, matchLevel);
                    }
                    continue;
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    if (matchLevel < 3) {
                        matchLevel = 3;
                        missException = new MissingAccessorException("Setter is abstract: " + method, matchLevel);
                    }
                    continue;
                }

                if (!allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Setter is not public: " + method, matchLevel);
                    }
                    continue;
                }

                if (!allowStatic && Modifier.isStatic(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Setter is static: " + method, matchLevel);
                    }
                    continue;
                }

                Class methodParameterType = method.getParameterTypes()[0];
                if (methodParameterType.isPrimitive() && propertyValue == null) {
                    if (matchLevel < 6) {
                        matchLevel = 6;
                        missException = new MissingAccessorException("Null can not be assigned to " +
                                methodParameterType.getName() + ": " + method, matchLevel);
                    }
                    continue;
                }


                if (!RecipeHelper.isInstance(methodParameterType, propertyValue) && !RecipeHelper.isConvertable(methodParameterType, propertyValue)) {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new MissingAccessorException((propertyValue == null ? "null" : propertyValue.getClass().getName()) + " can not be assigned or converted to " +
                                methodParameterType.getName() + ": " + method, matchLevel);
                    }
                    continue;
                }

                if (allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    setAccessible(method);
                }

                if (RecipeHelper.isInstance(methodParameterType, propertyValue)) {
                    // This setter requires no conversion, which means there can not be a conversion error.
                    // Therefore this setter is perferred and put a the head of the list
                    validSetters.addFirst(method);
                } else {
                    validSetters.add(method);
                }
            }

        }

        if (!validSetters.isEmpty()) {
            // remove duplicate methods (can happen with inheritance)
            return new ArrayList<Method>(new LinkedHashSet<Method>(validSetters));
        }
        
        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid setter method: ");
            buffer.append("public void ").append(typeClass.getName()).append(".");
            buffer.append(setterName).append("(");
            if (propertyValue == null) {
                buffer.append("null");
            } else if (propertyValue instanceof String || propertyValue instanceof Recipe) {
                buffer.append("...");
            } else {
                buffer.append(propertyValue.getClass().getName());
            }
            buffer.append(")");
            throw new MissingAccessorException(buffer.toString(), -1);
        }
    }

    public static List<Field> findAllFieldsByType(Class typeClass, Object propertyValue, Set<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (options == null) options = EnumSet.noneOf(Option.class);

        int matchLevel = 0;
        MissingAccessorException missException = null;

        List<Field> fields = new ArrayList<Field>(Arrays.asList(typeClass.getDeclaredFields()));
        Class parent = typeClass.getSuperclass();
        while (parent != null){
            fields.addAll(Arrays.asList(parent.getDeclaredFields()));
            parent = parent.getSuperclass();
        }

        boolean allowPrivate = options.contains(Option.PRIVATE_PROPERTIES);
        boolean allowStatic = options.contains(Option.STATIC_PROPERTIES);

        LinkedList<Field> validFields = new LinkedList<Field>();
        for (Field field : fields) {
            Class fieldType = field.getType();
            if (RecipeHelper.isInstance(fieldType, propertyValue) || RecipeHelper.isConvertable(fieldType, propertyValue)) {
                if (!allowPrivate && !Modifier.isPublic(field.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Field is not public: " + field, matchLevel);
                    }
                    continue;
                }

                if (!allowStatic && Modifier.isStatic(field.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Field is static: " + field, matchLevel);
                    }
                    continue;
                }


                if (fieldType.isPrimitive() && propertyValue == null) {
                    if (matchLevel < 6) {
                        matchLevel = 6;
                        missException = new MissingAccessorException("Null can not be assigned to " +
                                fieldType.getName() + ": " + field, matchLevel);
                    }
                    continue;
                }

                if (allowPrivate && !Modifier.isPublic(field.getModifiers())) {
                    setAccessible(field);
                }

                if (RecipeHelper.isInstance(fieldType, propertyValue)) {
                    // This field requires no conversion, which means there can not be a conversion error.
                    // Therefore this setter is perferred and put a the head of the list
                    validFields.addFirst(field);
                } else {
                    validFields.add(field);
                }
            }
        }

        if (!validFields.isEmpty()) {
            // remove duplicate methods (can happen with inheritance)
            return new ArrayList<Field>(new LinkedHashSet<Field>(validFields));
        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid field ");
            if (propertyValue instanceof Recipe) {
                buffer.append("for ").append(propertyValue == null ? "null" : propertyValue);
            } else {
                buffer.append("of type ").append(propertyValue == null ? "null" : propertyValue.getClass().getName());
            }
            buffer.append(" in class ").append(typeClass.getName());
            throw new MissingAccessorException(buffer.toString(), -1);
        }
    }
    public static List<Method> findAllSettersByType(Class typeClass, Object propertyValue, Set<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (options == null) options = EnumSet.noneOf(Option.class);

        int matchLevel = 0;
        MissingAccessorException missException = null;

        boolean allowPrivate = options.contains(Option.PRIVATE_PROPERTIES);
        boolean allowStatic = options.contains(Option.STATIC_PROPERTIES);

        LinkedList<Method> validSetters = new LinkedList<Method>();
        List<Method> methods = new ArrayList<Method>(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        for (Method method : methods) {
            if (method.getName().startsWith("set") && method.getParameterTypes().length == 1 && (RecipeHelper.isInstance(method.getParameterTypes()[0], propertyValue) || RecipeHelper.isConvertable(method.getParameterTypes()[0], propertyValue))) {
                if (method.getReturnType() != Void.TYPE) {
                    if (matchLevel < 2) {
                        matchLevel = 2;
                        missException = new MissingAccessorException("Setter returns a value: " + method, matchLevel);
                    }
                    continue;
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    if (matchLevel < 3) {
                        matchLevel = 3;
                        missException = new MissingAccessorException("Setter is abstract: " + method, matchLevel);
                    }
                    continue;
                }

                if (!allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Setter is not public: " + method, matchLevel);
                    }
                    continue;
                }

                Class methodParameterType = method.getParameterTypes()[0];
                if (methodParameterType.isPrimitive() && propertyValue == null) {
                    if (matchLevel < 6) {
                        matchLevel = 6;
                        missException = new MissingAccessorException("Null can not be assigned to " +
                                methodParameterType.getName() + ": " + method, matchLevel);
                    }
                    continue;
                }

                if (!allowStatic && Modifier.isStatic(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingAccessorException("Setter is static: " + method, matchLevel);
                    }
                    continue;
                }

                if (allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    setAccessible(method);
                }

                if (RecipeHelper.isInstance(methodParameterType, propertyValue)) {
                    // This setter requires no conversion, which means there can not be a conversion error.
                    // Therefore this setter is perferred and put a the head of the list
                    validSetters.addFirst(method);
                } else {
                    validSetters.add(method);
                }
            }

        }

        if (!validSetters.isEmpty()) {
            // remove duplicate methods (can happen with inheritance)
            return new ArrayList<Method>(new LinkedHashSet<Method>(validSetters));
        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid setter ");
            if (propertyValue instanceof Recipe) {
                buffer.append("for ").append(propertyValue == null ? "null" : propertyValue);
            } else {
                buffer.append("of type ").append(propertyValue == null ? "null" : propertyValue.getClass().getName());
            }
            buffer.append(" in class ").append(typeClass.getName());
            throw new MissingAccessorException(buffer.toString(), -1);
        }
    }

    public static ConstructorFactory findConstructor(Class typeClass, List<? extends Class<?>> parameterTypes, Set<Option> options) {
        return findConstructor(typeClass, null, parameterTypes, null, options);

    }
    public static ConstructorFactory findConstructor(Class typeClass, List<String> parameterNames, List<? extends Class<?>> parameterTypes, Set<String> availableProperties, Set<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (availableProperties == null) availableProperties = Collections.emptySet();
        if (options == null) options = EnumSet.noneOf(Option.class);

        //
        // verify that it is a class we can construct
        if (!Modifier.isPublic(typeClass.getModifiers())) {
            throw new ConstructionException("Class is not public: " + typeClass.getName());
        }
        if (Modifier.isInterface(typeClass.getModifiers())) {
            throw new ConstructionException("Class is an interface: " + typeClass.getName());
        }
        if (Modifier.isAbstract(typeClass.getModifiers())) {
            throw new ConstructionException("Class is abstract: " + typeClass.getName());
        }

        // verify parameter names and types are the same length
        if (parameterNames != null) {
            if (parameterTypes == null) parameterTypes = Collections.nCopies(parameterNames.size(), null);
            if (parameterNames.size() != parameterTypes.size()) {
                throw new ConstructionException("Invalid ObjectRecipe: recipe has " + parameterNames.size() +
                        " parameter names and " + parameterTypes.size() + " parameter types");
            }
        } else if (!options.contains(Option.NAMED_PARAMETERS)) {
            // Named parameters are not supported and no explicit parameters were given,
            // so we will only use the no-arg constructor
            parameterNames = Collections.emptyList();
            parameterTypes = Collections.emptyList();
        }


        // get all methods sorted so that the methods with the most constructor args are first
        List<Constructor> constructors = new ArrayList<Constructor>(Arrays.asList(typeClass.getConstructors()));
        constructors.addAll(Arrays.asList(typeClass.getDeclaredConstructors()));
        Collections.sort(constructors, new Comparator<Constructor>() {
            public int compare(Constructor constructor1, Constructor constructor2) {
                return constructor2.getParameterTypes().length - constructor1.getParameterTypes().length;
            }
        });

        // as we check each constructor, we remember the closest invalid match so we can throw a nice exception to the user
        int matchLevel = 0;
        MissingFactoryMethodException missException = null;

        boolean allowPrivate = options.contains(Option.PRIVATE_CONSTRUCTOR);
        for (Constructor constructor : constructors) {
            // if an explicit constructor is specified (via parameter types), look a constructor that matches
            if (parameterTypes != null) {
                if (constructor.getParameterTypes().length != parameterTypes.size()) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new MissingFactoryMethodException("Constructor has " + constructor.getParameterTypes().length + " arugments " +
                                "but expected " + parameterTypes.size() + " arguments: " + constructor);
                    }
                    continue;
                }

                if (!isAssignableFrom(parameterTypes, Arrays.<Class<?>>asList(constructor.getParameterTypes()))) {
                    if (matchLevel < 2) {
                        matchLevel = 2;
                        missException = new MissingFactoryMethodException("Constructor has signature " +
                                "public static " + typeClass.getName() + toParameterList(constructor.getParameterTypes()) +
                                " but expected signature " +
                                "public static " + typeClass.getName() + toParameterList(parameterTypes));
                    }
                    continue;
                }
            } else {
                // Implicit constructor selection based on named constructor args
                //
                // Only consider methods where we can supply a value for all of the parameters
                parameterNames = getParameterNames(constructor);
                if (parameterNames == null || !availableProperties.containsAll(parameterNames)) {
                    continue;
                }
            }

            if (Modifier.isAbstract(constructor.getModifiers())) {
                if (matchLevel < 4) {
                    matchLevel = 4;
                    missException = new MissingFactoryMethodException("Constructor is abstract: " + constructor);
                }
                continue;
            }

            if (!allowPrivate && !Modifier.isPublic(constructor.getModifiers())) {
                if (matchLevel < 5) {
                    matchLevel = 5;
                    missException = new MissingFactoryMethodException("Constructor is not public: " + constructor);
                }
                continue;
            }

            if (allowPrivate && !Modifier.isPublic(constructor.getModifiers())) {
                setAccessible(constructor);
            }

            return new ConstructorFactory(constructor, parameterNames);
        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid constructor: ");
            buffer.append("public void ").append(typeClass.getName()).append(toParameterList(parameterTypes));
            throw new ConstructionException(buffer.toString());
        }
    }

    public static StaticFactory findStaticFactory(Class typeClass, String factoryMethod, List<? extends Class<?>>  parameterTypes, Set<Option> options) {
        return findStaticFactory(typeClass, factoryMethod, null, parameterTypes, null, options);
    }

    public static StaticFactory findStaticFactory(Class typeClass, String factoryMethod, List<String> parameterNames, List<? extends Class<?>> parameterTypes, Set<String> allProperties, Set<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (factoryMethod == null) throw new NullPointerException("name is null");
        if (factoryMethod.length() == 0) throw new IllegalArgumentException("name is an empty string");
        if (allProperties == null) allProperties = Collections.emptySet();
        if (options == null) options = EnumSet.noneOf(Option.class);

        //
        // verify that it is a class we can construct
        if (!Modifier.isPublic(typeClass.getModifiers())) {
            throw new ConstructionException("Class is not public: " + typeClass.getName());
        }
        if (Modifier.isInterface(typeClass.getModifiers())) {
            throw new ConstructionException("Class is an interface: " + typeClass.getName());
        }

        // verify parameter names and types are the same length
        if (parameterNames != null) {
            if (parameterTypes == null) parameterTypes = Collections.nCopies(parameterNames.size(), null);
            if (parameterNames.size() != parameterTypes.size()) {
                throw new ConstructionException("Invalid ObjectRecipe: recipe has " + parameterNames.size() +
                        " parameter names and " + parameterTypes.size() + " parameter types");
            }
        } else if (!options.contains(Option.NAMED_PARAMETERS)) {
            // Named parameters are not supported and no explicit parameters were given,
            // so we will only use the no-arg constructor
            parameterNames = Collections.emptyList();
            parameterTypes = Collections.emptyList();
        }

        // get all methods sorted so that the methods with the most constructor args are first
        List<Method> methods = new ArrayList<Method>(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        Collections.sort(methods, new Comparator<Method>() {
            public int compare(Method method2, Method method1) {
                return method1.getParameterTypes().length - method2.getParameterTypes().length;
            }
        });


        // as we check each constructor, we remember the closest invalid match so we can throw a nice exception to the user
        int matchLevel = 0;
        MissingFactoryMethodException missException = null;

        boolean allowPrivate = options.contains(Option.PRIVATE_FACTORY);
        boolean caseInsesnitive = options.contains(Option.CASE_INSENSITIVE_FACTORY);
        for (Method method : methods) {
            // Only consider methods where the name matches
            if (!method.getName().equals(factoryMethod) && (!caseInsesnitive || !method.getName().equalsIgnoreCase(method.getName()))) {
                continue;
            }

            // if an explicit constructor is specified (via parameter types), look a constructor that matches
            if (parameterTypes != null) {
                if (method.getParameterTypes().length != parameterTypes.size()) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new MissingFactoryMethodException("Static factory method has " + method.getParameterTypes().length + " arugments " +
                                "but expected " + parameterTypes.size() + " arguments: " + method);
                    }
                    continue;
                }

                if (!isAssignableFrom(parameterTypes, Arrays.asList(method.getParameterTypes()))) {
                    if (matchLevel < 2) {
                        matchLevel = 2;
                        missException = new MissingFactoryMethodException("Static factory method has signature " +
                                "public static " + typeClass.getName() + "." + factoryMethod + toParameterList(method.getParameterTypes()) +
                                " but expected signature " +
                                "public static " + typeClass.getName() + "." + factoryMethod + toParameterList(parameterTypes));
                    }
                    continue;
                }
            } else {
                // Implicit constructor selection based on named constructor args
                //
                // Only consider methods where we can supply a value for all of the parameters
                parameterNames = getParameterNames(method);
                if (parameterNames == null || !allProperties.containsAll(parameterNames)) {
                    continue;
                }
            }

            if (method.getReturnType() == Void.TYPE) {
                if (matchLevel < 3) {
                    matchLevel = 3;
                    missException = new MissingFactoryMethodException("Static factory method does not return a value: " + method);
                }
                continue;
            }

            if (Modifier.isAbstract(method.getModifiers())) {
                if (matchLevel < 4) {
                    matchLevel = 4;
                    missException = new MissingFactoryMethodException("Static factory method is abstract: " + method);
                }
                continue;
            }

            if (!allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                if (matchLevel < 5) {
                    matchLevel = 5;
                    missException = new MissingFactoryMethodException("Static factory method is not public: " + method);
                }
                continue;
            }

            if (!Modifier.isStatic(method.getModifiers())) {
                if (matchLevel < 6) {
                    matchLevel = 6;
                    missException = new MissingFactoryMethodException("Static factory method is not static: " + method);
                }
                continue;
            }

            if (allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                setAccessible(method);
            }

            return new StaticFactory(method, parameterNames);
        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid factory method: ");
            buffer.append("public void ").append(typeClass.getName()).append(".");
            buffer.append(factoryMethod).append(toParameterList(parameterTypes));
            throw new MissingFactoryMethodException(buffer.toString());
        }
    }

    public static Method findInstanceFactory(Class typeClass, String factoryMethod, Set<Option> options) {
        if (typeClass == null) throw new NullPointerException("typeClass is null");
        if (factoryMethod == null) throw new NullPointerException("name is null");
        if (factoryMethod.length() == 0) throw new IllegalArgumentException("name is an empty string");
        if (options == null) options = EnumSet.noneOf(Option.class);
        
        int matchLevel = 0;
        MissingFactoryMethodException missException = null;

        boolean allowPrivate = options.contains(Option.PRIVATE_FACTORY);
        boolean caseInsesnitive = options.contains(Option.CASE_INSENSITIVE_FACTORY);

        List<Method> methods = new ArrayList<Method>(Arrays.asList(typeClass.getMethods()));
        methods.addAll(Arrays.asList(typeClass.getDeclaredMethods()));
        for (Method method : methods) {
            if (method.getName().equals(factoryMethod) || (caseInsesnitive && method.getName().equalsIgnoreCase(method.getName()))) {
                if (Modifier.isStatic(method.getModifiers())) {
                    if (matchLevel < 1) {
                        matchLevel = 1;
                        missException = new MissingFactoryMethodException("Instance factory method is static: " + method);
                    }
                    continue;
                }

                if (method.getParameterTypes().length != 0) {
                    if (matchLevel < 2) {
                        matchLevel = 2;
                        missException = new MissingFactoryMethodException("Instance factory method has signature " +
                                "public " + typeClass.getName() + "." + factoryMethod + toParameterList(method.getParameterTypes()) +
                                " but expected signature " +
                                "public " + typeClass.getName() + "." + factoryMethod + "()");
                    }
                    continue;
                }

                if (method.getReturnType() == Void.TYPE) {
                    if (matchLevel < 3) {
                        matchLevel = 3;
                        missException = new MissingFactoryMethodException("Instance factory method does not return a value: " + method);
                    }
                    continue;
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    if (matchLevel < 4) {
                        matchLevel = 4;
                        missException = new MissingFactoryMethodException("Instance factory method is abstract: " + method);
                    }
                    continue;
                }

                if (!allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    if (matchLevel < 5) {
                        matchLevel = 5;
                        missException = new MissingFactoryMethodException("Instance factory method is not public: " + method);
                    }
                    continue;
                }

                if (allowPrivate && !Modifier.isPublic(method.getModifiers())) {
                    setAccessible(method);
                }

                return method;
            }
        }

        if (missException != null) {
            throw missException;
        } else {
            StringBuffer buffer = new StringBuffer("Unable to find a valid factory method: ");
            buffer.append("public void ").append(typeClass.getName()).append(".");
            buffer.append(factoryMethod).append("()");
            throw new MissingFactoryMethodException(buffer.toString());
        }
    }

    public static List<String> getParameterNames(Constructor<?> constructor) {
        // use reflection to get Java6 ConstructorParameter annotation value
        try {
            Class<? extends Annotation> constructorPropertiesClass = ClassLoader.getSystemClassLoader().loadClass("java.beans.ConstructorProperties").asSubclass(Annotation.class);
            Annotation constructorProperties = constructor.getAnnotation(constructorPropertiesClass);
            if (constructorProperties != null) {
                String[] parameterNames = (String[]) constructorPropertiesClass.getMethod("value").invoke(constructorProperties);
                if (parameterNames != null) {
                    return Arrays.asList(parameterNames);
                }
            }
        } catch (Throwable e) {
        }

        ParameterNames parameterNames = constructor.getAnnotation(ParameterNames.class);
        if (parameterNames != null && parameterNames.value() != null) {
            return Arrays.asList(parameterNames.value());
        }
        if (parameterNamesLoader != null) {
            return parameterNamesLoader.get(constructor);
        }
        return null;
    }

    public static List<String> getParameterNames(Method method) {
        ParameterNames parameterNames = method.getAnnotation(ParameterNames.class);
        if (parameterNames != null && parameterNames.value() != null) {
            return Arrays.asList(parameterNames.value());
        }
        if (parameterNamesLoader != null) {
            return parameterNamesLoader.get(method);
        }
        return null;
    }

    public static interface Factory {
        List<String> getParameterNames();

        List<Type> getParameterTypes();

        Object create(Object... parameters) throws ConstructionException;
    }

    public static class ConstructorFactory implements Factory {
        private Constructor constructor;
        private List<String> parameterNames;

        public ConstructorFactory(Constructor constructor, List<String> parameterNames) {
            if (constructor == null) throw new NullPointerException("constructor is null");
            if (parameterNames == null) throw new NullPointerException("parameterNames is null");
            this.constructor = constructor;
            this.parameterNames = parameterNames;
        }

        public List<String> getParameterNames() {
            return parameterNames;
        }

        public List<Type> getParameterTypes() {
            return new ArrayList<Type>(Arrays.asList(constructor.getGenericParameterTypes()));
        }

        public Object create(Object... parameters) throws ConstructionException {
            // create the instance
            try {
                Object instance = constructor.newInstance(parameters);
                return instance;
            } catch (Exception e) {
                Throwable t = e;
                if (e instanceof InvocationTargetException) {
                    InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                    if (invocationTargetException.getCause() != null) {
                        t = invocationTargetException.getCause();
                    }
                }
                throw new ConstructionException("Error invoking constructor: " + constructor, t);
            }
        }
    }

    public static class StaticFactory implements Factory {
        private Method staticFactory;
        private List<String> parameterNames;

        public StaticFactory(Method staticFactory, List<String> parameterNames) {
            this.staticFactory = staticFactory;
            this.parameterNames = parameterNames;
        }

        public List<String> getParameterNames() {
            if (parameterNames == null) {
                throw new ConstructionException("InstanceFactory has not been initialized");
            }

            return parameterNames;
        }

        public List<Type> getParameterTypes() {
            return new ArrayList<Type>(Arrays.asList(staticFactory.getGenericParameterTypes()));
        }

        public Object create(Object... parameters) throws ConstructionException {
            try {
                Object instance = staticFactory.invoke(null, parameters);
                return instance;
            } catch (Exception e) {
                Throwable t = e;
                if (e instanceof InvocationTargetException) {
                    InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                    if (invocationTargetException.getCause() != null) {
                        t = invocationTargetException.getCause();
                    }
                }
                throw new ConstructionException("Error invoking factory method: " + staticFactory, t);
            }
        }
    }

    private static void setAccessible(final AccessibleObject accessibleObject) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                accessibleObject.setAccessible(true);
                return null;
            }
        });
    }

    private static String toParameterList(Class<?>[] parameterTypes) {
        return toParameterList(parameterTypes != null ? Arrays.asList(parameterTypes) : null);
    }

    private static String toParameterList(List<? extends Class<?>> parameterTypes) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("(");
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.size(); i++) {
                Class type = parameterTypes.get(i);
                if (i > 0) buffer.append(", ");
                buffer.append(type.getName());
            }
        } else {
            buffer.append("...");
        }
        buffer.append(")");
        return buffer.toString();
    }
}
