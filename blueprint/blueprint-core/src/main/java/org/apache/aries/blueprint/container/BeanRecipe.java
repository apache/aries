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
package org.apache.aries.blueprint.container;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.di.AbstractRecipe;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.proxy.CollaboratorFactory;
import org.apache.aries.blueprint.proxy.ProxyUtils;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.apache.aries.blueprint.utils.ReflectionUtils.PropertyDescriptor;
import org.apache.aries.blueprint.utils.generics.OwbParametrizedTypeImpl;
import org.apache.aries.blueprint.utils.generics.TypeInference;
import org.apache.aries.proxy.UnableToProxyException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.aries.blueprint.utils.ReflectionUtils.getRealCause;

/**
 * A <code>Recipe</code> to create POJOs.
 *
 * @version $Rev$, $Date$
 */
@SuppressWarnings("rawtypes")
public class BeanRecipe extends AbstractRecipe {

    static class UnwrapperedBeanHolder {
        final Object unwrapperedBean;
        final BeanRecipe recipe;

        public UnwrapperedBeanHolder(Object unwrapperedBean, BeanRecipe recipe) {
            this.unwrapperedBean = unwrapperedBean;
            this.recipe = recipe;
        }
    }

    public class VoidableCallable implements Callable<Object>, Voidable {

        private final AtomicReference<Object> ref = new AtomicReference<Object>();
        
        private final Semaphore sem = new Semaphore(1);
        
        private final ThreadLocal<Object> deadlockDetector = new ThreadLocal<Object>();
        
        public void voidReference() {
            ref.set(null);
        }

        public Object call() throws ComponentDefinitionException {
            Object o = ref.get();
            
            if (o == null) {
                if(deadlockDetector.get() != null) {
                    deadlockDetector.remove();
                    throw new ComponentDefinitionException("Construction cycle detected for bean " + name);
                }
                
                sem.acquireUninterruptibly();
                try {
                    o = ref.get();
                    if (o == null) {
                        deadlockDetector.set(this);
                        try {
                            o = internalCreate2();
                            ref.set(o);
                        } finally {
                            deadlockDetector.remove();
                        }
                    }
                } finally {
                  sem.release();
                }
            }
            
            return o;
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanRecipe.class);

    private final ExtendedBlueprintContainer blueprintContainer;
    private final LinkedHashMap<String,Object> properties = new LinkedHashMap<String,Object>();
    private final Object type;

    private String initMethod;
    private String destroyMethod;
    private List<Recipe> explicitDependencies;
    
    private Recipe factory;
    private String factoryMethod;
    private List<Object> arguments;
    private List<String> argTypes;
    private boolean reorderArguments;
    private final boolean allowFieldInjection;
    private final boolean allowRawConversion;
    private final boolean allowNonStandardSetters;
    private BeanMetadata interceptorLookupKey;
    

    public BeanRecipe(String name, ExtendedBlueprintContainer blueprintContainer, Object type, boolean allowFieldInjection, boolean allowRawConversion, boolean allowNonStandardSetters) {
        super(name);
        this.blueprintContainer = blueprintContainer;
        this.type = type;
        this.allowFieldInjection = allowFieldInjection;
        this.allowRawConversion = allowRawConversion;
        this.allowNonStandardSetters = allowNonStandardSetters;
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

    public void setFactoryMethod(String method) {
        this.factoryMethod = method;
    }
    
    public void setFactoryComponent(Recipe factory) {
        this.factory = factory;
    }
    
    public void setArgTypes(List<String> argTypes) {
        this.argTypes = argTypes;
    }
    
    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }
    
    public void setReorderArguments(boolean reorder) {
        this.reorderArguments = reorder;
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

    public List<Recipe> getExplicitDependencies() {
        return explicitDependencies;
    }

    public void setExplicitDependencies(List<Recipe> explicitDependencies) {
        this.explicitDependencies = explicitDependencies;
    }

    public void setInterceptorLookupKey(BeanMetadata metadata) {
    	interceptorLookupKey = metadata;
    }
    
    @Override
    public List<Recipe> getConstructorDependencies() {
        List<Recipe> recipes = new ArrayList<Recipe>();
        if (explicitDependencies != null) {
            recipes.addAll(explicitDependencies);
        }
        if (arguments != null) {
            for (Object argument : arguments) {
                if (argument instanceof Recipe) {
                    recipes.add((Recipe)argument);
                }
            }
        }
        return recipes;
    }
    
    public List<Recipe> getDependencies() {
        List<Recipe> recipes = new ArrayList<Recipe>();
        for (Object o : properties.values()) {
            if (o instanceof Recipe) {
                Recipe recipe = (Recipe) o;
                recipes.add(recipe);
            }
        }
        if (factory != null) {
            recipes.add(factory);
        }
        recipes.addAll(getConstructorDependencies());
        return recipes; 
    }

    private void instantiateExplicitDependencies() {
        if (explicitDependencies != null) {
            for (Recipe recipe : explicitDependencies) {
                recipe.create();
            }
        }
    }

    @Override
    protected Class loadClass(String className) {
        ClassLoader loader = type instanceof Class ? ((Class) type).getClassLoader() : null;
        ReifiedType t = loadType(className, loader);
        return t != null ? t.getRawClass() : null;
    }

    @Override
    protected ReifiedType loadType(String className) {
        return loadType(className, type instanceof Class ? ((Class) type).getClassLoader() : null);
    }

    private Object getInstance() throws ComponentDefinitionException {
        // Instanciate arguments
        List<Object> args = new ArrayList<Object>();
        List<ReifiedType> argTypes = new ArrayList<ReifiedType>();
        if (arguments != null) {
            for (int i = 0; i < arguments.size(); i++) {
                Object arg = arguments.get(i);
                if (arg instanceof Recipe) {
                    args.add(((Recipe) arg).create());
                } else {
                    args.add(arg);
                }
                if (this.argTypes != null) {
                    argTypes.add(this.argTypes.get(i) != null ? loadType(this.argTypes.get(i)) : null);
                }
            }
        }
        
        if (factory != null) {
            return getInstanceFromFactory(args, argTypes);
        } else if (factoryMethod != null) {
            return getInstanceFromStaticFactory(args, argTypes);
        } else {
            return getInstanceFromType(args, argTypes);
        }
        
    }
    
    private Object getInstanceFromFactory(List<Object> args, List<ReifiedType> argTypes) {
        Object factoryObj = getFactoryObj();
        
        // Map of matching methods
        Map<Method, List<Object>> matches = findMatchingMethods(factoryObj.getClass(), factoryMethod, true, args, argTypes);
        if (matches.size() == 1) {
            try {
                Map.Entry<Method, List<Object>> match = matches.entrySet().iterator().next();
                return invoke(match.getKey(), factoryObj, match.getValue().toArray());
            } catch (Throwable e) {
                throw wrapAsCompDefEx(e);
            }
        } else if (matches.size() == 0) {
            throw new ComponentDefinitionException("Unable to find a matching factory method " + factoryMethod + " on class " + factoryObj.getClass().getName() + " for arguments " + argsToString(args) + " when instanciating bean " + getName());
        } else {
            throw new ComponentDefinitionException("Multiple matching factory methods " + factoryMethod + " found on class " + factoryObj.getClass().getName() + " for arguments " + argsToString(args) + " when instanciating bean " + getName() + ": " + matches.keySet());
        }
    }

    private Object getFactoryObj() {
        // look for instance method on factory object
        Object factoryObj = factory.create();
        
        // If the factory is a service reference, we need to get hold of the actual proxy for the service
        if (factoryObj instanceof ReferenceRecipe.ServiceProxyWrapper) {
            try {
                factoryObj = ((ReferenceRecipe.ServiceProxyWrapper) factoryObj).convert(new ReifiedType(Object.class));
            } catch (Exception e) {
                throw wrapAsCompDefEx(e);
            }
        } else if (factoryObj instanceof UnwrapperedBeanHolder) {
            factoryObj = wrap((UnwrapperedBeanHolder) factoryObj, Object.class);
        }
        return factoryObj;
    }
    
    private Object getInstanceFromStaticFactory(List<Object> args, List<ReifiedType> argTypes) {
        // Map of matching methods
        Map<Method, List<Object>> matches = findMatchingMethods(getType(), factoryMethod, false, args, argTypes);
        if (matches.size() == 1) {
            try {
                Map.Entry<Method, List<Object>> match = matches.entrySet().iterator().next();
                return invoke(match.getKey(), null, match.getValue().toArray());
            } catch (Throwable e) {
                throw wrapAsCompDefEx(e);
            }
        } else if (matches.size() == 0) {
            throw new ComponentDefinitionException("Unable to find a matching factory method " + factoryMethod + " on class " + getTypeName() + " for arguments " + argsToString(args) + " when instanciating bean " + getName());
        } else {
            throw new ComponentDefinitionException("Multiple matching factory methods " + factoryMethod + " found on class " + getTypeName() + " for arguments " + argsToString(args) + " when instanciating bean " + getName() + ": " + matches.keySet());
        }
    }

    private Object getInstanceFromType(List<Object> args, List<ReifiedType> argTypes) {
        if (getType() == null) {
            throw new ComponentDefinitionException("No factoryMethod nor class is defined for this bean");
        }
        // Map of matching constructors
        Map<Constructor<?>, List<Object>> matches = findMatchingConstructors(getType(), args, argTypes);
        if (matches.size() == 1) {
            try {
                Map.Entry<Constructor<?>, List<Object>> match = matches.entrySet().iterator().next();
                return newInstance(match.getKey(), match.getValue().toArray());
            } catch (Throwable e) {
                throw wrapAsCompDefEx(e);
            }
        } else if (matches.size() == 0) {
            throw new ComponentDefinitionException("Unable to find a matching constructor on class " + getTypeName() + " for arguments " + argsToString(args) + " when instanciating bean " + getName());
        } else {
            throw new ComponentDefinitionException("Multiple matching constructors found on class " + getTypeName() + " for arguments " + argsToString(args) + " when instanciating bean " + getName() + ": " + matches.keySet());
        }
    }

    private ComponentDefinitionException wrapAsCompDefEx(Throwable e) {
        return new ComponentDefinitionException("Error when instantiating bean " + getName() + " of class " + getTypeName(), getRealCause(e));
    }

    private String getTypeName() {
        Class<?> type = getType();
        return type == null ? null : type.getName();
    }

    private Map<Constructor<?>, List<Object>> findMatchingConstructors(Class type, List<Object> args, List<ReifiedType> types) {
        List<TypeInference.TypedObject> targs = getTypedObjects(args, types);
        TypeInference.Converter cnv = new TIConverter();
        List<TypeInference.Match<Constructor<?>>> m = TypeInference.findMatchingConstructors(type, targs, cnv, reorderArguments);
        if (!m.isEmpty()) {
            int score = m.iterator().next().getScore();
            final Iterator<TypeInference.Match<Constructor<?>>> each = m.iterator();
            while (each.hasNext()) {
                if (each.next().getScore() > score) {
                    each.remove();
                }
            }
        }
        Map<Constructor<?>, List<Object>> map = new HashMap<Constructor<?>, List<Object>>();
        for (TypeInference.Match<Constructor<?>> match : m) {
            List<Object> nargs = new ArrayList<Object>();
            for (TypeInference.TypedObject to : match.getArgs()) {
                nargs.add(to.getValue());
            }
            map.put(match.getMember(), nargs);
        }
        return map;
    }

    private Map<Method, List<Object>> findMatchingMethods(Class type, String name, boolean instance, List<Object> args, List<ReifiedType> types) {
        List<TypeInference.TypedObject> targs = getTypedObjects(args, types);
        TypeInference.Converter cnv = new TIConverter();
        List<TypeInference.Match<Method>> m;
        if (instance) {
            m = TypeInference.findMatchingMethods(type, name, targs, cnv, reorderArguments);
        } else {
            m = TypeInference.findMatchingStatics(type, name, targs, cnv, reorderArguments);
        }
        if (!m.isEmpty()) {
            int score = m.iterator().next().getScore();
            final Iterator<TypeInference.Match<Method>> each = m.iterator();
            while (each.hasNext()) {
                if (each.next().getScore() > score) {
                    each.remove();
                }
            }
        }
        Map<Method, List<Object>> map = new HashMap<Method, List<Object>>();
        for (TypeInference.Match<Method> match : m) {
            List<Object> nargs = new ArrayList<Object>();
            for (TypeInference.TypedObject to : match.getArgs()) {
                nargs.add(to.getValue());
            }
            map.put(match.getMember(), nargs);
        }
        return map;
    }

    protected Object convert(Object obj, Type from, Type to) throws Exception {
        if (allowRawConversion
                && (from instanceof ParameterizedType || to instanceof ParameterizedType)
                && GenericType.getConcreteClass(from) == GenericType.getConcreteClass(to)) {
            boolean assignable = true;
            if (from instanceof ParameterizedType) {
                for (Type t : ((ParameterizedType) from).getActualTypeArguments()) {
                    assignable &= t == Object.class;
                }
            }
            if (to instanceof ParameterizedType) {
                for (Type t : ((ParameterizedType) to).getActualTypeArguments()) {
                    assignable &= t == Object.class;
                }
            }
            if (assignable) {
                return obj instanceof UnwrapperedBeanHolder
                        ? ((UnwrapperedBeanHolder) obj).unwrapperedBean : obj;
            }
        }
        return convert(obj, to);
    }

    private class TIConverter implements TypeInference.Converter {
        public TypeInference.TypedObject convert(TypeInference.TypedObject from, Type to) throws Exception {
            Object arg = BeanRecipe.this.convert(from.getValue(), from.getType(), to);
            return new TypeInference.TypedObject(to, arg);
        }
    }

    private Type toType(ReifiedType rt) {
        if (rt.size() > 0) {
            Type[] at = new Type[rt.size()];
            for (int j = 0; j < at.length; j++) {
                at[j] = toType(rt.getActualTypeArgument(j));
            }
            return new OwbParametrizedTypeImpl(null, rt.getRawClass(), at);
        } else {
            return rt.getRawClass();
        }
    }

    private List<TypeInference.TypedObject> getTypedObjects(List<Object> args, List<ReifiedType> types) {
        List<TypeInference.TypedObject> targs = new ArrayList<TypeInference.TypedObject>();
        for (int i = 0; i < args.size(); i++) {
            Type t;
            Object o = args.get(i);
            ReifiedType rt = types.get(i);
            if (rt == null && o != null) {
                Object ot = o instanceof UnwrapperedBeanHolder ? ((UnwrapperedBeanHolder) o).unwrapperedBean : o;
                rt = new GenericType(ot.getClass());
            }
            if (rt != null) {
                if (rt.size() == 0 && rt.getRawClass().getTypeParameters().length > 0 && rt.getRawClass() != Class.class) {
                    GenericType[] tt = new GenericType[rt.getRawClass().getTypeParameters().length];
                    Arrays.fill(tt, 0, tt.length, new GenericType(Object.class));
                    rt = new GenericType(rt.getRawClass(), tt);
                }
                t = toType(rt);
            } else {
                t = null;
            }

            targs.add(new TypeInference.TypedObject(t, o));
        }
        return targs;
    }

    /**
     * Returns init method (if any). Throws exception if the init-method was set explicitly on the bean
     * and the method is not found on the instance.
     */
    protected Method getInitMethod(Object instance) throws ComponentDefinitionException {
        Method method = null;        
        if (initMethod != null && initMethod.length() > 0) {
            method = ReflectionUtils.getLifecycleMethod(instance.getClass(), initMethod);
            if (method == null) {
                throw new ComponentDefinitionException("Component '" + getName() + "' does not have init-method: " + initMethod);
            }
        }
        return method;
    }

    /**
     * Returns destroy method (if any). Throws exception if the destroy-method was set explicitly on the bean
     * and the method is not found on the instance.
     */
    public Method getDestroyMethod(Object instance) throws ComponentDefinitionException {
        Method method = null;        
        if (instance != null && destroyMethod != null && destroyMethod.length() > 0) {
            method = ReflectionUtils.getLifecycleMethod(instance.getClass(), destroyMethod);
            if (method == null) {
                throw new ComponentDefinitionException("Component '" + getName() + "' does not have destroy-method: " + destroyMethod);
            }
        }
        return method;
    }
    
    /**
     * Small helper class, to construct a chain of BeanCreators.
     * <br> 
     * Each bean creator in the chain will return a bean that has been 
     * processed by every BeanProcessor in the chain before it.
     */
    private static class BeanCreatorChain implements BeanProcessor.BeanCreator {
        public enum ChainType{Before,After};
        private final BeanProcessor.BeanCreator parentBeanCreator;
        private final BeanProcessor parentBeanProcessor;
        private final BeanMetadata beanData;
        private final String beanName;        
        private final ChainType when;
        public BeanCreatorChain(BeanProcessor.BeanCreator parentBeanCreator, 
                                BeanProcessor parentBeanProcessor,
                                BeanMetadata beanData,
                                String beanName,
                                ChainType when) {
            this.parentBeanCreator = parentBeanCreator;
            this.parentBeanProcessor = parentBeanProcessor;
            this.beanData = beanData;
            this.beanName = beanName;
            this.when = when;
        }

        public Object getBean() {
            Object previousBean = parentBeanCreator.getBean();
            Object processed = null;
            switch (when) {
                case Before:
                    processed = parentBeanProcessor.beforeInit(previousBean, beanName, parentBeanCreator, beanData);
                    break;
                case After:
                    processed = parentBeanProcessor.afterInit(previousBean, beanName, parentBeanCreator, beanData);
                    break;
            }
            return processed;
        }   
    }
    
    private Object runBeanProcPreInit(Object obj) {
        String beanName = getName();
        BeanMetadata beanData = (BeanMetadata) blueprintContainer
          .getComponentDefinitionRegistry().getComponentDefinition(beanName);        
        List<BeanProcessor> processors = blueprintContainer.getProcessors(BeanProcessor.class);
        
        //The start link of the chain, that provides the 
        //original, unprocessed bean to the head of the chain.
        BeanProcessor.BeanCreator initialBeanCreator = new BeanProcessor.BeanCreator() {            
            public Object getBean() {
                Object obj = getInstance();
                //getinit, getdestroy, addpartial object don't need calling again.
                //however, property injection does.
                setProperties(obj);
                return obj;
            }
        };

        BeanProcessor.BeanCreator currentCreator = initialBeanCreator;
        for (BeanProcessor processor : processors) {
            obj = processor.beforeInit(obj, getName(), currentCreator, beanData);
            currentCreator = new BeanCreatorChain(currentCreator, processor, beanData, beanName, BeanCreatorChain.ChainType.Before);
        }
        return obj;
    }
    
    private void runBeanProcInit(Method initMethod, Object obj){
        // call init method
        if (initMethod != null) {
            try {
                invoke(initMethod, obj, (Object[]) null);
            } catch (Throwable t) {
                throw new ComponentDefinitionException("Unable to initialize bean " + getName(), getRealCause(t));
            }
        }   
    }
    
    private Object runBeanProcPostInit(Object obj) {
        String beanName = getName();
        BeanMetadata beanData = (BeanMetadata) blueprintContainer
                .getComponentDefinitionRegistry().getComponentDefinition(beanName);
        List<BeanProcessor> processors = blueprintContainer.getProcessors(BeanProcessor.class);
        
        //The start link of the chain, that provides the 
        //original, unprocessed bean to the head of the chain.
        BeanProcessor.BeanCreator initialBeanCreator = new BeanProcessor.BeanCreator() {            
            public Object getBean() {                                
                Object obj = getInstance();
                //getinit, getdestroy, addpartial object don't need calling again.
                //however, property injection does.
                setProperties(obj);
                //as this is the post init chain, new beans need to go thru 
                //the pre-init chain, and then have init called, before 
                //being passed along the post-init chain.
                obj = runBeanProcPreInit(obj);
                runBeanProcInit(getInitMethod(obj), obj);
                return obj;
            }
        };

        BeanProcessor.BeanCreator currentCreator = initialBeanCreator;
        for (BeanProcessor processor : processors) {
            obj = processor.afterInit(obj, getName(), currentCreator, beanData);
            currentCreator = new BeanCreatorChain(currentCreator, processor, beanData, beanName, BeanCreatorChain.ChainType.After);
        }
        return obj;
    }    
    
    private Object addInterceptors(final Object original, Collection<Class<?>> requiredInterfaces)
            throws ComponentDefinitionException {

        Object intercepted = null;
        if (requiredInterfaces.isEmpty())
            requiredInterfaces.add(original.getClass());

        ComponentDefinitionRegistry reg = blueprintContainer
                .getComponentDefinitionRegistry();
        List<Interceptor> interceptors = reg.getInterceptors(interceptorLookupKey);
        if (interceptors != null && interceptors.size() > 0) {
            try {
                Bundle b = FrameworkUtil.getBundle(original.getClass());
                if (b == null) {
                    // we have a class from the framework parent, so use our bundle for proxying.
                    b = blueprintContainer.getBundleContext().getBundle();
                }
                intercepted = blueprintContainer.getProxyManager().createInterceptingProxy(b,
                        requiredInterfaces, original, CollaboratorFactory.create(interceptorLookupKey, interceptors));
            } catch (org.apache.aries.proxy.UnableToProxyException e) {
                Bundle b = blueprintContainer.getBundleContext().getBundle();
                throw new ComponentDefinitionException("Unable to create proxy for bean " + name + " in bundle " + b.getSymbolicName() + "/" + b.getVersion(), e);
            }
        } else {
            intercepted = original;
        }
        return intercepted;
    }
        
    @Override
    protected Object internalCreate() throws ComponentDefinitionException {
        if (factory instanceof ReferenceRecipe) {
            ReferenceRecipe rr = (ReferenceRecipe) factory;
            if (rr.getProxyChildBeanClasses() != null) {
                return createProxyBean(rr);
            }
        } 
        return new UnwrapperedBeanHolder(internalCreate2(), this);
    }
    
    private Object createProxyBean(ReferenceRecipe rr) {
        try {
            VoidableCallable vc = new VoidableCallable();
            rr.addVoidableChild(vc);
            return blueprintContainer.getProxyManager().createDelegatingProxy(
                blueprintContainer.getBundleContext().getBundle(), rr.getProxyChildBeanClasses(),
                vc, vc.call());
        } catch (UnableToProxyException e) {
            throw new ComponentDefinitionException(e);
        }
    }
    
    private Object internalCreate2() throws ComponentDefinitionException {
        
        instantiateExplicitDependencies();

        Object obj = getInstance();
                
        // check for init lifecycle method (if any)
        Method initMethod = getInitMethod(obj);
        
        // check for destroy lifecycle method (if any)
        getDestroyMethod(obj);
        
        // Add partially created object to the container
//        if (initMethod == null) {
            addPartialObject(obj);
//        }

        // inject properties
        setProperties(obj);
        
        obj = runBeanProcPreInit(obj);
        
        runBeanProcInit(initMethod, obj);
        
        obj = runBeanProcPostInit(obj);
        
        //Replaced by calling wrap on the UnwrapperedBeanHolder
//        obj = addInterceptors(obj);
        
        return obj;
    }
    
    static Object wrap(UnwrapperedBeanHolder holder, Collection<Class<?>> requiredViews) {
        return holder.recipe.addInterceptors(holder.unwrapperedBean, requiredViews);
    }
    
    static Object wrap(UnwrapperedBeanHolder holder, Class<?> requiredView) {
        if (requiredView == Object.class) {
            //We don't know what we need so we have to do everything
            return holder.recipe.addInterceptors(holder.unwrapperedBean, new ArrayList<Class<?>>(1));
        } else {
            return holder.recipe.addInterceptors(holder.unwrapperedBean, ProxyUtils.asList(requiredView));
        }
    }
    
    
    @Override
    public void destroy(Object obj) {
        if (!(obj instanceof UnwrapperedBeanHolder)) {
            LOGGER.warn("Object to be destroyed is not an instance of UnwrapperedBeanHolder, type: " + obj);
            return;
        }
    
    
        obj = ((UnwrapperedBeanHolder) obj).unwrapperedBean;
        for (BeanProcessor processor : blueprintContainer.getProcessors(BeanProcessor.class)) {
            processor.beforeDestroy(obj, getName());
        }
        try {
            Method method = getDestroyMethod(obj);
            if (method != null) {
                invoke(method, obj, (Object[]) null);
            }
        } catch (ComponentDefinitionException e) {
            // This exception occurs if the destroy method does not exist, so we just output the exception message.
            LOGGER.error(e.getMessage());
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            BundleContext ctx = blueprintContainer.getBundleContext();
            Bundle b = ctx.getBundle();
            LOGGER.error("The blueprint bean {} in bundle {}/{} incorrectly threw an exception from its destroy method.", getName(), b.getSymbolicName(), b.getVersion(), t);
        } catch (Exception e) {
            BundleContext ctx = blueprintContainer.getBundleContext();
            Bundle b = ctx.getBundle();
            LOGGER.error("An exception occurred while calling the destroy method of the blueprint bean  in bundle {}/{}.", getName(), b.getSymbolicName(), b.getVersion(), getRealCause(e));
        }
        for (BeanProcessor processor : blueprintContainer.getProcessors(BeanProcessor.class)) {
            processor.afterDestroy(obj, getName());
        }
    }

    public void setProperties(Object instance) throws ComponentDefinitionException {
        // clone the properties so they can be used again
        Map<String, Object> propertyValues = new LinkedHashMap<String, Object>(properties);
        setProperties(propertyValues, instance, instance.getClass());
    }

    public Class getType() {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof String) {
            return loadClass((String) type);
        } else {
            return null;
        }
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
            PropertyDescriptor pd = getPropertyDescriptor(clazz, names[i]);
            if (pd.allowsGet()) {
                try {
                    instance = pd.get(instance, blueprintContainer);
                } catch (Exception e) {
                    throw new ComponentDefinitionException("Error getting property: " + names[i] + " on bean " + getName() + " when setting property " + propertyName + " on class " + clazz.getName(), getRealCause(e));
                }
                if (instance == null) {
                    throw new ComponentDefinitionException("Error setting compound property " + propertyName + " on bean " + getName() + ". Property " + names[i] + " is null");
                }
                clazz = instance.getClass();
            } else {
                throw new ComponentDefinitionException("No getter for " + names[i] + " property on bean " + getName() + " when setting property " + propertyName + " on class " + clazz.getName());
            }
        }
        
        // Instantiate value
        if (propertyValue instanceof Recipe) {
            propertyValue = ((Recipe) propertyValue).create();
        }

        final PropertyDescriptor pd = getPropertyDescriptor(clazz, names[names.length - 1]);
        if (pd.allowsSet()) {
            try {
                Object val = propertyValue instanceof UnwrapperedBeanHolder
                        ? ((UnwrapperedBeanHolder) propertyValue).unwrapperedBean
                        : propertyValue;
                pd.set(instance, val, blueprintContainer);
            } catch (Exception e) {
                throw new ComponentDefinitionException("Error setting property: " + pd, getRealCause(e));
            }
        } else {
            throw new ComponentDefinitionException("No setter for " + names[names.length - 1] + " property");
        }
    }

    private ReflectionUtils.PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String name) {
        for (ReflectionUtils.PropertyDescriptor pd : ReflectionUtils.getPropertyDescriptors(clazz, allowFieldInjection, allowNonStandardSetters)) {
            if (pd.getName().equals(name)) {
                return pd;
            }
        }
        throw new ComponentDefinitionException("Unable to find property descriptor " + name + " on class " + clazz.getName());
    }
        
    private Object invoke(Method method, Object instance, Object... args) throws Exception {
        return ReflectionUtils.invoke(blueprintContainer.getAccessControlContext(), method, instance, args);        
    }
    
    private Object newInstance(Constructor constructor, Object... args) throws Exception {
        return ReflectionUtils.newInstance(blueprintContainer.getAccessControlContext(), constructor, args);         
    }

    private String argsToString(List<Object> args) {
        Iterator<Object> it = args.iterator();
        if (!it.hasNext())
            return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (;;) {
            Object e = it.next();
            if (e instanceof UnwrapperedBeanHolder) {
                e = ((UnwrapperedBeanHolder) e).unwrapperedBean;
            }
            sb.append(e).append(" (").append(e.getClass()).append(")");
            if (!it.hasNext())
                return sb.append(']').toString();
            sb.append(',').append(' ');
        }
    }
    
    private static Object UNMATCHED = new Object();

    private class ArgumentMatcher {

        private final List<TypeEntry> entries;
        private final boolean convert;

        public ArgumentMatcher(Type[] types, boolean convert) {
            entries = new ArrayList<TypeEntry>();
            for (Type type : types) {
                entries.add(new TypeEntry(new GenericType(type)));
            }
            this.convert = convert;
        }

        public List<Object> match(List<Object> arguments, List<ReifiedType> forcedTypes) {
            if (find(arguments, forcedTypes)) {
                return getArguments();
            }
            return null;
        }

        private List<Object> getArguments() {
            List<Object> list = new ArrayList<Object>();
            for (TypeEntry entry : entries) {
                if (entry.argument == UNMATCHED) {
                    throw new RuntimeException("There are unmatched generics");
                } else {
                    list.add(entry.argument);
                }
            }
            return list;
        }

        private boolean find(List<Object> arguments, List<ReifiedType> forcedTypes) {
            if (entries.size() == arguments.size()) {
                boolean matched = true;
                for (int i = 0; i < arguments.size() && matched; i++) {
                    matched = find(arguments.get(i), forcedTypes.get(i));
                }
                return matched;
            }
            return false;
        }

        private boolean find(Object arg, ReifiedType forcedType) {
            for (TypeEntry entry : entries) {
                Object val = arg;
                if (entry.argument != UNMATCHED) {
                    continue;
                }
                if (forcedType != null) {
                    if (!forcedType.equals(entry.type)) {
                        continue;
                    }
                } else if (arg != null) {
                    if (convert) {
                        
                        if (canConvert(arg, entry.type)) {
                            try {
                                val = convert(arg, entry.type);
                            } catch (Exception e) {
                                throw new ComponentDefinitionException(e);
                            }
                        } else {
                            continue;
                        }
                    } else {
                        UnwrapperedBeanHolder holder = null;
                        if (arg instanceof UnwrapperedBeanHolder) {
                            holder = (UnwrapperedBeanHolder) arg;
                            arg = holder.unwrapperedBean;
                        }
                        if (!AggregateConverter.isAssignable(arg, entry.type)) {
                            continue;
                        } else if (holder != null) {
                            val = wrap(holder, entry.type.getRawClass());
                        }
                    }
                }
                entry.argument = val;
                return true;
            }
            return false;
        }

    }

    private static class TypeEntry {

        private final ReifiedType type;
        private Object argument;

        public TypeEntry(ReifiedType type) {
            this.type = type;
            this.argument = UNMATCHED;
        }

    }

}
