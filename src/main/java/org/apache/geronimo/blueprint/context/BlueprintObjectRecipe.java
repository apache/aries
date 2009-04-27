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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.RecipeHelper;
import org.apache.xbean.recipe.ReferenceRecipe;
import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.geronimo.blueprint.utils.ArgumentsMatch;
import org.apache.geronimo.blueprint.utils.ArgumentsMatcher;
import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class BlueprintObjectRecipe extends ObjectRecipe {

    private final BlueprintContextImpl blueprintContext;
    private boolean keepRecipe = false;
    private String initMethod;
    private String destroyMethod;
    private List<String> explicitDependencies;
    
    private Object factory; // could be Recipe or actual object
    private String factoryMethod;
    private List<Object> arguments;
    private List<BeanArgument> beanArguments;
    private boolean reorderArguments;
    
    public BlueprintObjectRecipe(BlueprintContextImpl blueprintContext, Class typeName) {
        super(typeName);
        this.blueprintContext = blueprintContext;
    }
    
    public void setFactoryMethod(String method) {
        this.factoryMethod = method;
    }
    
    public void setFactoryComponent(Object factory) {
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
        List<Recipe> recipes = super.getNestedRecipes();
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

    private List<Object> getInitialArguments(boolean refAllowed) throws ConstructionException {
        List<Object> args = new ArrayList<Object>();
        for (int i = 0; beanArguments != null && i < beanArguments.size(); i++) {
            BeanArgument argument = beanArguments.get(i);
            Class type = loadClass(argument.getValueType());
            Object obj = arguments.get(i);
            if (obj == null) {
                obj = new NullRecipe(type);
            } else if (obj instanceof Recipe) {                
                if (type != null || shouldPreinstantiate(argument.getValue())) {
                    obj = RecipeHelper.convert(Object.class, obj, refAllowed);
                    obj = convert(obj, type);
                }
            } else {
                obj = convert(obj, type);
            }
            args.add(obj);
        }

        return args;
    }
    
    private boolean shouldPreinstantiate(Metadata metadata) {
        if (metadata instanceof ValueMetadata) {
            ValueMetadata stringValue = (ValueMetadata) metadata;
            return (stringValue.getTypeName() != null);
        }
        return true;
    }
    
    private Class loadClass(String typeName) throws ConstructionException {
        if (typeName == null) {
            return null;
        }
        try {
            return Instanciator.loadClass(blueprintContext, typeName);
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Unable to load type class " + typeName);
        }
    }
    
    private Object convert(Object source, Class type) throws ConstructionException {
        if (type != null && blueprintContext != null) {
            try {
                source = blueprintContext.getConversionService().convert(source, type);
            } catch (Exception e) {
                throw new ConstructionException("Failed to convert", e);
            }            
        }
        return source;
    }
    
    private List<Object> getFinalArguments(ArgumentsMatch match, boolean refAllowed) throws ConstructionException {
        List<Object> arguments = match.getArguments();
        Class[] parameterTypes = match.getParameterTypes();
        
        List<Object> args = new ArrayList<Object>();
        for (int i = 0; i < arguments.size(); i++) {
            Object argument = arguments.get(i);
            if (argument instanceof Recipe) {
                argument = RecipeHelper.convert(parameterTypes[i], argument, refAllowed);
            }
            args.add(argument);
        }
        
        return args;
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
            Object factoryObj = RecipeHelper.convert(Object.class, factory, refAllowed);
            options.add(ArgumentsMatcher.Option.INSTANCE_METHODS_ONLY);
            ArgumentsMatch match = ArgumentsMatcher.findMethod(factoryObj.getClass(), factoryMethod, arguments, options);
            // convert parameters
            List<Object> args = getFinalArguments(match, refAllowed);
            // invoke instance method
            try {
                instance = match.getMethod().invoke(factoryObj, args.toArray());
            } catch (InvocationTargetException e) {
                throw new ConstructionException(e);
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
                throw new ConstructionException(e);
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
                throw new ConstructionException(e);
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
    
    @Override
    public List<Recipe> getConstructorRecipes() {
        return getNestedRecipes();
    }
    
    @Override
    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        
        final Object obj = getInstance(lazyRefAllowed);
        
        // check for init lifecycle method (if any)
        Method initMethod = getInitMethod(obj);
        
        // check for destroy lifecycle method (if any)
        getDestroyMethod(obj);
        
        // inject properties
        setProperties(obj);
        
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
        
        if (getName() != null) {
            ExecutionContext.getContext().addObject(getName(), obj);
        }
        
        return obj;
    }
    
    public void destroyInstance(Object obj) {
        try {
            Method method = getDestroyMethod(obj);
            if (method != null) {
                method.invoke(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
