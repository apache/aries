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

import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.ObjectRecipe;

/**
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class BlueprintObjectRecipe extends ObjectRecipe {
    
    private boolean keepRecipe = false;
    private Method initMethod;
    private Method destroyMethod;
    
    public BlueprintObjectRecipe(Class typeName) {
        super(typeName);
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
        
    @Override
    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        Object obj = super.internalCreate(expectedType, lazyRefAllowed);
        if (initMethod != null) {
            try {
                initMethod.invoke(obj, new Object[] {});
            } catch (InvocationTargetException e) {
                Throwable root = e.getTargetException();
                throw new ConstructionException("init-method generated exception", root);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return obj;
    }
    
    public void destroyInstance(Object obj) {
        if (!getType().equals(obj.getClass())) {
            throw new RuntimeException("");
        }
        if (destroyMethod != null) {
            try {
                destroyMethod.invoke(obj, new Object[] {});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
