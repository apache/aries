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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.blueprint.BeanProcessor;
import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.geronimo.blueprint.utils.ArgumentsMatch;
import org.apache.geronimo.blueprint.utils.ArgumentsMatcher;
import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.apache.xbean.recipe.AbstractRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.xbean.recipe.Option;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.RecipeHelper;
import static org.apache.xbean.recipe.RecipeHelper.toClass;
import org.apache.xbean.recipe.ReferenceRecipe;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

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
    
    private List<Object> getInitialArguments(boolean refAllowed) throws ConstructionException {
        List<Object> args = new ArrayList<Object>();
        for (int i = 0; beanArguments != null && i < beanArguments.size(); i++) {
            BeanArgument argument = beanArguments.get(i);
            String valueType = argument.getValueType();          
            Class type = loadClass(valueType);
            Object obj = arguments.get(i);
            if (type != null) {
                obj = new TypedRecipe(blueprintContext.getConversionService(), type, obj);
            } else {
                if (obj == null) {
                    obj = new TypedRecipe();
                } else if (obj instanceof Recipe) {                
                    if (shouldPreinstantiate(argument.getValue())) {
                        obj = ((Recipe) obj).create(Object.class, refAllowed);
                    }
                }
            }
            args.add(obj);
        }

        return args;
    }
    
    private List<Object> getFinalArguments(ArgumentsMatch match, boolean refAllowed) throws ConstructionException {
        List<Object> arguments = match.getArguments();
        Type[] parameterTypes = match.getGenericParameterTypes();

        List<Object> args = new ArrayList<Object>();
        for (int i = 0; i < arguments.size(); i++) {
            Object argument = arguments.get(i);
            if (argument instanceof Recipe) {
                argument = ((Recipe) argument).create(parameterTypes[i], refAllowed);
            }
            args.add(argument);
        }

        return args;
    }

    private boolean shouldPreinstantiate(Metadata metadata) {
        if (metadata instanceof ValueMetadata) {
            ValueMetadata stringValue = (ValueMetadata) metadata;
            return (stringValue.getTypeName() != null);
        } else if (metadata instanceof MapMetadata) {
            return false;
        } else if (metadata instanceof CollectionMetadata) {
            return false;
        } else if (metadata instanceof PropsMetadata) {
            return false;
        }
        return true;
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
        
    private Set<ArgumentsMatcher.Option> getArgumentsMatcherOptions() {
        Set<ArgumentsMatcher.Option> options = new HashSet<ArgumentsMatcher.Option>();
        if (reorderArguments) {
            options.add(ArgumentsMatcher.Option.ARGUMENT_REORDER);
        }
        return options;
    }
    
    private Object getInstance(boolean refAllowed) throws ConstructionException {
        Object instance = null;
        
        Set<ArgumentsMatcher.Option> options = getArgumentsMatcherOptions();
        List<Object> arguments = getInitialArguments(refAllowed);
        
        if (factory != null) {
            // look for instance method on factory object
            Object factoryObj = factory.create(Object.class, refAllowed);
            options.add(ArgumentsMatcher.Option.INSTANCE_METHODS_ONLY);
            ArgumentsMatch match = ArgumentsMatcher.findMethod(factoryObj.getClass(), factoryMethod, arguments, options);
            // convert parameters
            List<Object> args = getFinalArguments(match, refAllowed);
            // invoke instance method
            try {
                instance = match.getMethod().invoke(factoryObj, args.toArray());
            } catch (InvocationTargetException e) {
                Throwable root = e.getTargetException();
                throw new ConstructionException(root);
            } catch (Exception e) {
                throw new ConstructionException(e);
            }
        } else if (factoryMethod != null) {
            // look for static method on this object
            options.add(ArgumentsMatcher.Option.STATIC_METHODS_ONLY);
            ArgumentsMatch match = ArgumentsMatcher.findMethod(getType(), factoryMethod, arguments, options);
            // convert parameters
            List<Object> args = getFinalArguments(match, refAllowed);
            // invoke static method
            try {
                instance = match.getMethod().invoke(null, args.toArray());
            } catch (InvocationTargetException e) {
                Throwable root = e.getTargetException();
                throw new ConstructionException(root);
            } catch (Exception e) {
                throw new ConstructionException(e);
            }
        } else {
            // look for constructor on this object
            ArgumentsMatch match = ArgumentsMatcher.findConstructor(getType(), arguments, options);
            // convert parameters
            List<Object> args = getFinalArguments(match, refAllowed);
            // invoke constructor
            try {
                instance = match.getConstructor().newInstance(args.toArray());
            } catch (InvocationTargetException e) {
                Throwable root = e.getTargetException();
                throw new ConstructionException(root);
            } catch (Exception e) {
                throw new ConstructionException(e);
            }
        }
        
        return instance;
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
    
    public boolean canCreate(Type type) {
        if (factoryMethod == null) {
            Class myType = getType();
            return RecipeHelper.isAssignable(type, myType) || RecipeHelper.isAssignable(type, myType);
        } else {
            // factory-method was specified, so we're not really sure what type of object we create
            // until we actually create it
            // TODO: is it possible to perform eager disambiguation on the factory method to get
            //   the return type?
            // TODO: this stuff should be moved to getType() instead
            return true;
        }
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
                propertyValue = blueprintContext.getConversionService().convert(propertyValue, toClass(type));
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

}
