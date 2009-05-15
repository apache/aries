/*
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
package org.apache.geronimo.blueprint.context;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.blueprint.BeanProcessor;
import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.geronimo.blueprint.utils.ConversionUtils;
import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.apache.xbean.recipe.AbstractRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.xbean.recipe.Option;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.RecipeHelper;
import org.apache.xbean.recipe.ReferenceRecipe;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.reflect.BeanArgument;

/**
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class BlueprintObjectRecipe extends AbstractRecipe {

    private Class typeClass;
    private final LinkedHashMap<String,Object> properties = new LinkedHashMap<String,Object>();
    private final EnumSet<Option> options = EnumSet.noneOf(Option.class);

    private final BlueprintContextImpl blueprintContext;
    private boolean keepRecipe = false;
    private String initMethod;
    private String destroyMethod;
    private List<String> explicitDependencies;
    
    private Recipe factory; // could be Recipe or actual object
    private String factoryMethod;
    private List<Object> arguments;
    private List<BeanArgument> beanArguments;
    private boolean reorderArguments;
    
    public BlueprintObjectRecipe(BlueprintContextImpl blueprintContext, Class typeClass) {
        this.typeClass = typeClass;
        this.blueprintContext = blueprintContext;
        allow(Option.LAZY_ASSIGNMENT);
    }
    
    public void allow(Option option){
        options.add(option);
    }

    public void disallow(Option option){
        options.remove(option);
    }

    public Set<Option> getOptions() {
        return Collections.unmodifiableSet(options);
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public Map<String, Object> getProperties() {
        return new LinkedHashMap<String, Object>(properties);
    }

    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    public void setAllProperties(Map<?,?> map) {
        if (map == null) throw new NullPointerException("map is null");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            setProperty(name, value);
        }
    }

    public void setFactoryMethod(String method) {
        this.factoryMethod = method;
    }
    
    public void setFactoryComponent(Recipe factory) {
        this.factory = factory;
    }
    
    public void setBeanArguments(List<BeanArgument> arguments) {
        this.beanArguments = arguments;
    }
    
    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }
    
    public void setReorderArguments(boolean reorder) {
        this.reorderArguments = reorder;
    }
    
    public void setKeepRecipe(boolean keepRecipe) {
        this.keepRecipe = keepRecipe;
    }
    
    public boolean getKeepRecipe() {
        return keepRecipe;
    }
    
    public void setInitMethod(String initMethod) {
        this.initMethod = initMethod;
    }
    
    public String getInitMethod() {
        return initMethod;
    }
    
    public void setDestroyMethod(String destroyMethod) {
        this.destroyMethod = destroyMethod;
    }
    
    public String getDestroyMethod() {
        return destroyMethod;
    }

    public List<String> getExplicitDependencies() {
        return explicitDependencies;
    }

    public void setExplicitDependencies(List<String> explicitDependencies) {
        this.explicitDependencies = explicitDependencies;
    }

    @Override
    public List<Recipe> getNestedRecipes() {
        List<Recipe> recipes = new ArrayList<Recipe>();
        for (Object o : properties.values()) {
            if (o instanceof Recipe) {
                Recipe recipe = (Recipe) o;
                recipes.add(recipe);
            }
        }
        if (arguments != null) {
            for (Object argument : arguments) {
                if (argument instanceof Recipe) {
                    recipes.add((Recipe)argument);
                }
            }
        }
        if (explicitDependencies != null) {
            for (String name : explicitDependencies) {
                recipes.add(new ReferenceRecipe(name));
            }
        }
        return recipes; 
    }

    private void instantiateExplicitDependencies() {
        if (explicitDependencies != null) {
            for (String name : explicitDependencies) {
                Recipe recipe = new ReferenceRecipe(name);
                recipe.create(Object.class, false);
            }
        }
    }
    
    private Class loadClass(String typeName) throws ConstructionException {
        if (typeName == null) {
            return null;
        }
        try {
            return RecipeBuilder.loadClass(blueprintContext, typeName);
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Unable to load type class " + typeName);
        }
    }
        
    private Object getInstance(boolean refAllowed) throws ConstructionException {
        Object instance = null;
        
        // Instanciate arguments
        List<Object> args = new ArrayList<Object>();
        List<Class> types = new ArrayList<Class>();
        if (arguments != null) {
            for (int i = 0; i < arguments.size(); i++) {
                Object arg = arguments.get(i);
                if (arg instanceof Recipe) {
                    args.add(((Recipe) arg).create(Object.class, refAllowed));
                } else {
                    args.add(arg);
                }
                String valueType = beanArguments.get(i).getValueType();
                if (valueType != null) {
                    try {
                        types.add(loadClass(valueType));
                    } catch (Throwable t) {
                        throw new ConstructionException("Error loading class " + valueType + " when instanciating bean " + getName());
                    }
                } else {
                    types.add(null);
                }
            }
        }

        if (factory != null) {
            // look for instance method on factory object
            Object factoryObj = factory.create(Object.class, false);
            // Map of matching methods
            Map<Method, List<Object>> matches = findMatchingMethods(factoryObj.getClass(), factoryMethod, true, args, types, arguments);
            if (matches.size() == 1) {
                try {
                    Map.Entry<Method, List<Object>> match = matches.entrySet().iterator().next();
                    instance = match.getKey().invoke(factoryObj, match.getValue().toArray());
                } catch (InvocationTargetException e) {
                    Throwable root = e.getTargetException();
                    throw new ConstructionException("Error when instanciating bean " + getName() + " of class " + getType(), root);
                } catch (Throwable e) {
                    throw new ConstructionException("Error when instanciating bean " + getName() + " of class " + getType(), e);
                }
            } else if (matches.size() == 0) {
                throw new ConstructionException("Unable to find a matching factory method " + factoryMethod + " on class " + factoryObj.getClass() + " for arguments " + args + " when instanciating bean " + getName());
            } else {
                throw new ConstructionException("Multiple matching factory methods " + factoryMethod + " found on class " + factoryObj.getClass() + " for arguments " + args + " when instanciating bean " + getName());
            }
        } else if (factoryMethod != null) {
            // Map of matching methods
            Map<Method, List<Object>> matches = findMatchingMethods(getType(), factoryMethod, false, args, types, arguments);
            if (matches.size() == 1) {
                try {
                    Map.Entry<Method, List<Object>> match = matches.entrySet().iterator().next();
                    instance = match.getKey().invoke(null, match.getValue().toArray());
                } catch (InvocationTargetException e) {
                    Throwable root = e.getTargetException();
                    throw new ConstructionException("Error when instanciating bean " + getName() + " of class " + getType(), root);
                } catch (Throwable e) {
                    throw new ConstructionException("Error when instanciating bean " + getName() + " of class " + getType(), e);
                }
            } else if (matches.size() == 0) {
                throw new ConstructionException("Unable to find a matching factory method " + factoryMethod + " on class " + getType() + " for arguments " + args + " when instanciating bean " + getName());
            } else {
                throw new ConstructionException("Multiple matching factory methods " + factoryMethod + " found on class " + getType() + " for arguments " + args + " when instanciating bean " + getName());
            }
        } else {
            // Map of matching constructors
            Map<Constructor, List<Object>> matches = findMatchingConstructors(args, types, args);
            if (matches.size() == 1) {
                try {
                    Map.Entry<Constructor, List<Object>> match = matches.entrySet().iterator().next();
                    instance = match.getKey().newInstance(match.getValue().toArray());
                } catch (InvocationTargetException e) {
                    Throwable root = e.getTargetException();
                    throw new ConstructionException("Error when instanciating bean " + getName() + " of class " + getType(), root);
                } catch (Throwable e) {
                    throw new ConstructionException("Error when instanciating bean " + getName() + " of class " + getType(), e);
                }
            } else if (matches.size() == 0) {
                throw new ConstructionException("Unable to find a matching constructor on class " + getType() + " for arguments " + args + " when instanciating bean " + getName());
            } else {
                throw new ConstructionException("Multiple matching constructors found on class " + getType() + " for arguments " + args + " when instanciating bean " + getName());
            }
        }
        
        return instance;
    }

    private Object convert(Object obj, Type type) throws Exception {
        return ConversionUtils.convert(obj, type, blueprintContext.getConversionService());
    }

    private Map<Method, List<Object>> findMatchingMethods(Class type, String name, boolean instance, List<Object> args, List<Class> types, List<Object> arguments) {
        Map<Method, List<Object>> matches = new HashMap<Method, List<Object>>();
        // Get constructors
        List<Method> methods = new ArrayList<Method>(Arrays.asList(type.getMethods()));
        // Discard any signature with wrong cardinality
        for (Iterator<Method> it = methods.iterator(); it.hasNext();) {
            Method mth = it.next();
            if (!mth.getName().equals(name)) {
                it.remove();
            } else if (mth.getParameterTypes().length != args.size()) {
                it.remove();
            } else if (!instance ^ Modifier.isStatic(mth.getModifiers())) {
                it.remove();
            }
        }
        // Find a direct match
        for (Method mth : methods) {
            boolean found = true;
            List<Object> match = new ArrayList<Object>();
            for (int i = 0; i < args.size(); i++) {
                if (types.get(i) != null && types.get(i) != mth.getParameterTypes()[i]) {
                    found = false;
                    break;
                }
                try {
                    Object val = convert(args.get(i), mth.getGenericParameterTypes()[i]);
                    match.add(val);
                } catch (Throwable t) {
                    found = false;
                    break;
                }
            }
            if (found) {
                matches.put(mth, match);
            }
        }
        // Start reordering
        if (matches.size() != 1 && reorderArguments && arguments.size() > 1) {
            Map<Method, List<Object>> nmatches = new HashMap<Method, List<Object>>();
            for (Method mth : methods) {
                ArgumentMatcher matcher = new ArgumentMatcher(mth.getGenericParameterTypes());
                List<Object> match = matcher.match(args, types);
                if (match != null) {
                    nmatches.put(mth, match);
                }
            }
            if (nmatches.size() > 0) {
                matches = nmatches;
            }
        }
        return matches;
    }

    private Map<Constructor, List<Object>> findMatchingConstructors(List<Object> args, List<Class> types, List<Object> arguments) {
        Map<Constructor, List<Object>> matches = new HashMap<Constructor, List<Object>>();
        // Get constructors
        List<Constructor> constructors = new ArrayList<Constructor>(Arrays.asList(getType().getConstructors()));
        // Discard any signature with wrong cardinality
        for (Iterator<Constructor> it = constructors.iterator(); it.hasNext();) {
            if (it.next().getParameterTypes().length != args.size()) {
                it.remove();
            }
        }
        // Find a direct match
        for (Constructor cns : constructors) {
            boolean found = true;
            List<Object> match = new ArrayList<Object>();
            for (int i = 0; i < args.size(); i++) {
                if (types.get(i) != null && types.get(i) != cns.getParameterTypes()[i]) {
                    found = false;
                    break;
                }
                try {
                    Object val = convert(args.get(i), cns.getGenericParameterTypes()[i]);
                    match.add(val);
                } catch (Throwable t) {
                    found = false;
                    break;
                }
            }
            if (found) {
                matches.put(cns, match);
            }
        }
        // Start reordering
        if (matches.size() != 1 && reorderArguments && arguments.size() > 1) {
            Map<Constructor, List<Object>> nmatches = new HashMap<Constructor, List<Object>>();
            for (Constructor cns : constructors) {
                ArgumentMatcher matcher = new ArgumentMatcher(cns.getGenericParameterTypes());
                List<Object> match = matcher.match(args, types);
                if (match != null) {
                    nmatches.put(cns, match);
                }
            }
            if (nmatches.size() > 0) {
                matches = nmatches;
            }
        }
        return matches;
    }

    /**
     * Returns init method (if any). Throws exception if the init-method was set explicitly on the bean
     * and the method is not found on the instance.
     */
    protected Method getInitMethod(Object instance) throws ConstructionException {
        Method method = null;        
        if (initMethod == null) {
            ComponentDefinitionRegistryImpl registry = blueprintContext.getComponentDefinitionRegistry();
            method = ReflectionUtils.getLifecycleMethod(instance.getClass(), registry.getDefaultInitMethod());
        } else if (initMethod.length() > 0) {
            method = ReflectionUtils.getLifecycleMethod(instance.getClass(), initMethod);
            if (method == null) {
                throw new ConstructionException("Component '" + getName() + "' does not have init-method: " + initMethod);
            }
        }
        return method;
    }

    /**
     * Returns destroy method (if any). Throws exception if the destroy-method was set explicitly on the bean
     * and the method is not found on the instance.
     */
    protected Method getDestroyMethod(Object instance) throws ConstructionException {
        Method method = null;        
        if (destroyMethod == null) {
            ComponentDefinitionRegistryImpl registry = blueprintContext.getComponentDefinitionRegistry();
            method = ReflectionUtils.getLifecycleMethod(instance.getClass(), registry.getDefaultDestroyMethod());
        } else if (destroyMethod.length() > 0) {
            method = ReflectionUtils.getLifecycleMethod(instance.getClass(), destroyMethod);
            if (method == null) {
                throw new ConstructionException("Component '" + getName() + "' does not have destroy-method: " + destroyMethod);
            }
        }
        return method;
    }
    
    @Override
    public List<Recipe> getConstructorRecipes() {
        return getNestedRecipes();
    }
    
    @Override
    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        
        instantiateExplicitDependencies();

        Object obj = getInstance(lazyRefAllowed);
        
        // check for init lifecycle method (if any)
        Method initMethod = getInitMethod(obj);
        
        // check for destroy lifecycle method (if any)
        getDestroyMethod(obj);
        
        // inject properties
        setProperties(obj);

        for (BeanProcessor processor : blueprintContext.getBeanProcessors()) {
            obj = processor.beforeInit(obj, getName());
        }
        
        // call init method
        if (initMethod != null) {
            try {
                initMethod.invoke(obj);
            } catch (InvocationTargetException e) {
                Throwable root = e.getTargetException();
                throw new ConstructionException("init-method generated exception", root);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (getName() != null && !keepRecipe) {
            ExecutionContext.getContext().addObject(getName(), obj);
        }
        
        return obj;
    }
    
    public void destroyInstance(Object obj) {
        for (BeanProcessor processor : blueprintContext.getBeanProcessors()) {
            processor.beforeDestroy(obj, getName());
        }
        try {
            Method method = getDestroyMethod(obj);
            if (method != null) {
                method.invoke(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (BeanProcessor processor : blueprintContext.getBeanProcessors()) {
            processor.afterDestroy(obj, getName());
        }
    }

    public Type[] getTypes() {
        Class type = getType();
        if (type != null) {
            return new Type[] { getType() };
        } else{
            return new Type[] { Object.class };
        }
    }

    public void setProperties(Object instance) throws ConstructionException {
        // clone the properties so they can be used again
        Map<String,Object> propertyValues = new LinkedHashMap<String,Object>(properties);
        setProperties(propertyValues, instance, instance.getClass());
    }

    public Class getType() {
        return typeClass;
    }

    private void setProperties(Map<String, Object> propertyValues, Object instance, Class clazz) {
        // set remaining properties
        for (Map.Entry<String, Object> entry : propertyValues.entrySet()) {
            String propertyName = entry.getKey();
            Object propertyValue = entry.getValue();

            setProperty(instance, clazz, propertyName, propertyValue);
        }

    }

    private void setProperty(Object instance, Class clazz, String propertyName, Object propertyValue) {
        String[] names = propertyName.split("\\.");
        for (int i = 0; i < names.length - 1; i++) {
            Method getter = getPropertyDescriptor(clazz, names[i]).getReadMethod();
            if (getter != null) {
                try {
                    instance = getter.invoke(instance);
                    clazz = instance.getClass();
                } catch (Exception e) {
                    Throwable t = e;
                    if (e instanceof InvocationTargetException) {
                        InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                        if (invocationTargetException.getCause() != null) {
                            t = invocationTargetException.getCause();
                        }
                    }
                    throw new ConstructionException("Error getting property: " + names[i] + " on bean " + getName() + " when setting property " + propertyName + " on class " + clazz.getName(), t);
                }
            } else {
                throw new ConstructionException("No getter for " + names[i] + " property");
            }
        }
        Method setter = getPropertyDescriptor(clazz, names[names.length - 1]).getWriteMethod();
        if (setter != null) {
            // convert the value to type of setter/field
            Type type = setter.getGenericParameterTypes()[0];
            // Instanciate value
            if (propertyValue instanceof Recipe) {
                propertyValue = ((Recipe) propertyValue).create(type, false);
            }
            try {
                propertyValue = convert(propertyValue, type);
            } catch (Exception e) {
                String valueType = propertyValue == null ? "null" : propertyValue.getClass().getName();
                String memberType = type instanceof Class ? ((Class) type).getName() : type.toString();
                throw new ConstructionException("Unable to convert property value" +
                        " from " + valueType +
                        " to " + memberType +
                        " for injection " + setter, e);
            }
            try {
                // set value
                setter.invoke(instance, propertyValue);
            } catch (Exception e) {
                Throwable t = e;
                if (e instanceof InvocationTargetException) {
                    InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                    if (invocationTargetException.getCause() != null) {
                        t = invocationTargetException.getCause();
                    }
                }
                throw new ConstructionException("Error setting property: " + setter, t);
            }
        } else {
            throw new ConstructionException("No setter for " + names[names.length - 1] + " property");
        }
    }

    PropertyDescriptor getPropertyDescriptor(Class clazz, String name) {
        // TODO: it seems to fail in some cases, for example if there are two setters and no getters
        //    it should throw an exception
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (pd.getName().equals(name)) {
                    return pd;
                }
            }
            throw new ConstructionException("Unable to find property descriptor " + name + " on class " + clazz.getName());
        } catch (IntrospectionException e) {
            throw new ConstructionException("Unable to find property descriptor " + name + " on class " + clazz.getName(), e);
        }
    }

    private static Object UNMATCHED = new Object();

    private class ArgumentMatcher {

        private ConversionService converter;
        private List<TypeEntry> entries;

        public ArgumentMatcher(Type[] types) {
            entries = new ArrayList<TypeEntry>();
            for (Type type : types) {
                entries.add(new TypeEntry(type));
            }
        }

        public List<Object> match(List<Object> arguments, List<Class> forcedTypes) {
            if (find(arguments, forcedTypes)) {
                return getArguments();
            }
            return null;
        }

        private List<Object> getArguments() {
            List<Object> list = new ArrayList<Object>();
            for (TypeEntry entry : entries) {
                if (entry.argument == UNMATCHED) {
                    throw new RuntimeException("There are unmatched types");
                } else {
                    list.add(entry.argument);
                }
            }
            return list;
        }

        private boolean find(List<Object> arguments, List<Class> forcedTypes) {
            if (entries.size() == arguments.size()) {
                boolean matched = true;
                for (int i = 0; i < arguments.size() && matched; i++) {
                    matched = find(arguments.get(i), forcedTypes.get(i));
                }
                return matched;
            }
            return false;
        }

        private boolean find(Object arg, Class forcedType) {
            for (TypeEntry entry : entries) {
                Object val = arg;
                if (entry.argument != UNMATCHED) {
                    continue;
                }
                if (forcedType != null) {
                    if (forcedType != entry.type) {
                        continue;
                    }
                } else if (arg != null) {
                    try {
                        val = convert(arg, entry.type);
                    } catch (Throwable t) {
                        continue;
                    }
                }
                entry.argument = val;
                return true;
            }
            return false;
        }

    }

    private static class TypeEntry {

        private final Type type;
        private Object argument;

        public TypeEntry(Type type) {
            this.type = type;
            this.argument = UNMATCHED;
        }

    }

}
