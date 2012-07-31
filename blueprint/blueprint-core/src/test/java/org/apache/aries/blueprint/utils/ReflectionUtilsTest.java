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
package org.apache.aries.blueprint.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;

import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.di.CircularDependencyException;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.utils.ReflectionUtils.PropertyDescriptor;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ReifiedType;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {
    private PropertyDescriptor[] sut;
    private final ExtendedBlueprintContainer mockBlueprint = Skeleton.newMock(
            new Object() {
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    return Thread.currentThread().getContextClassLoader().loadClass(name);
                }
            },            
            ExtendedBlueprintContainer.class);
    
    public static class GetterOnly {
        public String getValue() { return "test"; }
    }
    
    private class Inconvertible {}
    
    @BeforeClass
    public static void before()
    {
        ExecutionContext.Holder.setContext(new ExecutionContext() {
            public void addPartialObject(String name, Object object) {}
            public boolean containsObject(String name) { return false; }

            public Object convert(Object value, ReifiedType type) throws Exception {
                if (type.getRawClass().equals(Inconvertible.class)) throw new Exception();
                else if (type.getRawClass().equals(String.class)) return String.valueOf(value);
                else if (type.getRawClass().equals(List.class)) {
                    if (value == null) return null;
                    else if (value instanceof Collection) return new ArrayList((Collection) value);
                    else throw new Exception();
                } else if (value == null) return null;
                else if (type.getRawClass().isInstance(value)) return value;
                else throw new Exception();
            }
            
            public boolean canConvert(Object value, ReifiedType type) {
                if (value instanceof Inconvertible) return false;
                else if (type.getRawClass().equals(String.class)) return true;
                else if (type.getRawClass().equals(List.class) && (value == null || value instanceof Collection)) return true;
                else return false;
            }

            public Object getObject(String name) { return null; }
            public Object getPartialObject(String name) { return null; }
            public Recipe getRecipe(String name) { return null; }
            public Class loadClass(String className) throws ClassNotFoundException { return null; }
            public Recipe pop() { return null; }
            public void push(Recipe recipe) throws CircularDependencyException {}
            public void removePartialObject(String name) {}
            public Future<Object> addFullObject(String name, Future<Object> object) { return null; }            
        });
    }
    
    @Test
    public void testGetterOnly() throws Exception {
        loadProps(GetterOnly.class, true);
        
        assertEquals(2, sut.length);
        assertEquals("class", sut[0].getName());
        assertEquals("value", sut[1].getName());
        
        assertTrue(sut[1].allowsGet());
        assertFalse(sut[1].allowsSet());
        
        assertEquals("test", sut[1].get(new GetterOnly(), mockBlueprint));
    }
    
    public static class SetterOnly {
        private String f;
        
        public void setField(String val) { f = val; }
        public String retrieve() { return f; }
    }
    
    @Test
    public void testSetterOnly() throws Exception {
        loadProps(SetterOnly.class, false);
        
        assertEquals(2, sut.length);
        assertEquals("field", sut[1].getName());
        
        assertFalse(sut[1].allowsGet());
        assertTrue(sut[1].allowsSet());
        
        SetterOnly so = new SetterOnly();
        sut[1].set(so, "trial", mockBlueprint);
        assertEquals("trial", so.retrieve());
    }
    
    public static class SetterAndGetter {
        private String f;
        
        public void setField(String val) { f = val; }
        public String getField() { return f; }
    }
    
    @Test
    public void testSetterAndGetter() throws Exception {
        loadProps(SetterAndGetter.class, false);
        
        assertEquals(2, sut.length);
        assertEquals("field", sut[1].getName());
        
        assertTrue(sut[1].allowsGet());
        assertTrue(sut[1].allowsSet());
        
        SetterAndGetter sag = new SetterAndGetter();
        sut[1].set(sag, "tribulation", mockBlueprint);
        assertEquals("tribulation", sut[1].get(sag, mockBlueprint));
    }
    
    static class DuplicateGetter {
        public boolean isField() { return true; }
        public boolean getField() { return false; }
    }
    
    @Test
    public void testDuplicateGetter() {
        loadProps(DuplicateGetter.class, false);
        
        assertEquals(1, sut.length);
        assertEquals("class", sut[0].getName());
    }
    
    static class FieldsAndProps {
        private String hidden = "ordeal";
        private String nonHidden;
        
        public String getHidden() { return hidden; }
    }
    
    @Test
    public void testFieldsAndProps() throws Exception {
        loadProps(FieldsAndProps.class, true);
        
        assertEquals(3, sut.length);
        
        FieldsAndProps fap = new FieldsAndProps();
        
        // no mixing of setter and field injection
        assertEquals("hidden", sut[1].getName());
        assertTrue(sut[1].allowsGet());
        assertTrue(sut[1].allowsSet());
        
        assertEquals("ordeal", sut[1].get(fap, mockBlueprint));
        sut[1].set(fap, "calvary", mockBlueprint);
        assertEquals("calvary", sut[1].get(fap, mockBlueprint));
        
        assertEquals("nonHidden", sut[2].getName());
        assertTrue(sut[2].allowsGet());
        assertTrue(sut[2].allowsSet());
        
        sut[2].set(fap, "predicament", mockBlueprint);
        assertEquals("predicament", sut[2].get(fap, mockBlueprint));
    }
    
    public static class OverloadedSetters {
        public Object field;
        
        public void setField(String val) { field = val; }
        public void setField(List<String> val) { field = val; }
    }
    
    @Test
    public void testOverloadedSetters() throws Exception {
        loadProps(OverloadedSetters.class, false);
        
        OverloadedSetters os = new OverloadedSetters();

        sut[1].set(os, "scrutiny", mockBlueprint);
        assertEquals("scrutiny", os.field);
        
        sut[1].set(os, Arrays.asList("evaluation"), mockBlueprint);
        assertEquals(Arrays.asList("evaluation"), os.field);
        
        // conversion case, Integer -> String
        sut[1].set(os, new Integer(3), mockBlueprint);
        assertEquals("3", os.field);
    }
    
    @Test(expected=ComponentDefinitionException.class)
    public void testApplicableSetter() throws Exception {
        loadProps(OverloadedSetters.class, false);
        
        sut[1].set(new OverloadedSetters(), new Inconvertible(), mockBlueprint);
    }
    
    public static class MultipleMatchesByConversion {
        public void setField(String s) {}
        public void setField(List<String> list) {}
    }
    
    @Test(expected=ComponentDefinitionException.class)
    public void testMultipleMatchesByConversion() throws Exception {
        loadProps(MultipleMatchesByConversion.class, false);
        
        sut[1].set(new MultipleMatchesByConversion(), new HashSet<String>(), mockBlueprint);
    }

    public static class MultipleMatchesByType {
        public void setField(List<String> list) {}
        public void setField(Queue<String> list) {}
        
        public static int field;
        
        public void setOther(Collection<String> list) { field=1; }
        public void setOther(List<String> list) { field=2; }
    }
    
    @Test(expected=ComponentDefinitionException.class)
    public void testMultipleSettersMatchByType() throws Exception {
        loadProps(MultipleMatchesByType.class, false);
        
        sut[1].set(new MultipleMatchesByType(), new LinkedList<String>(), mockBlueprint);
    }
    
    @Test
    public void testDisambiguationByHierarchy() throws Exception {
        loadProps(MultipleMatchesByType.class, false);
        
        sut[2].set(new MultipleMatchesByType(), new ArrayList<String>(), mockBlueprint);
        assertEquals(2, MultipleMatchesByType.field);
    }

    public static class NullSetterDisambiguation {
        public static int field;
        
        public void setField(int i) { field = i; }
        public void setField(Integer i) { field = -1; }
    }
    
    @Test
    public void testNullDisambiguation() throws Exception {
        loadProps(NullSetterDisambiguation.class, false);
        
        sut[1].set(new NullSetterDisambiguation(), null, mockBlueprint);
        assertEquals(-1, NullSetterDisambiguation.field);
    }
    
    private void loadProps(Class<?> clazz, boolean allowsFieldInjection)
    {
        List<PropertyDescriptor> props = new ArrayList<PropertyDescriptor>(
                Arrays.asList(ReflectionUtils.getPropertyDescriptors(clazz, allowsFieldInjection)));
        
        Collections.sort(props, new Comparator<PropertyDescriptor>() {
            public int compare(PropertyDescriptor o1, PropertyDescriptor o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        
        sut = props.toArray(new PropertyDescriptor[0]);
    }
}
