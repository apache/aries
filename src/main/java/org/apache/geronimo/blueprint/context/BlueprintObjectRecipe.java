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
import org.apache.geronimo.blueprint.Destroyable;
import org.apache.geronimo.blueprint.utils.ArgumentsMatch;
import org.apache.geronimo.blueprint.utils.ArgumentsMatcher;
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
    private Method initMethod;
    private Method destroyMethod;
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
    
    public void setInitMethod(Method initMethod) {
        this.initMethod = initMethod;
    }
    
    public Method getInitMethod() {
        return initMethod;
    }
    
    public void setDestroyMethod(Method destroyMethod) {
        this.destroyMethod = destroyMethod;
    }
    
    public Method getDestroyMethod() {
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
            Object obj = arguments.get(i);
            if (obj instanceof Recipe) {
                if (shouldPreinstantiate(argument.getValue())) {
                    obj = RecipeHelper.convert(Object.class, obj, refAllowed);
                    obj = convert(obj, argument.getValueType());
                }
            } else {
                obj = convert(obj, argument.getValueType());
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
    
    private Object convert(Object source, String typeName) throws ConstructionException {
        Class type = null;
        try {
            type = Instanciator.loadClass(blueprintContext, typeName);
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Unable to load type class " + typeName);
        }
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
    
    @Override
    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        
        final Object obj = getInstance(lazyRefAllowed);
        
        // inject properties
        setProperties(obj);
        
        if (getName() != null) {
            ExecutionContext.getContext().addObject(getName(), obj);
        }
        
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
        if (destroyMethod != null && blueprintContext != null) {
            Destroyable d = new Destroyable() {
                public void destroy() {
                    destroyInstance(obj);
                }
            };
            blueprintContext.addDestroyable(getName(), d);
        }
        return obj;
    }
    
    public void destroyInstance(Object obj) {
        if (!getType().equals(obj.getClass())) {
            throw new RuntimeException("");
        }
        if (destroyMethod != null) {
            try {
                destroyMethod.invoke(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
