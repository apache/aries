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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.di.PassThroughRecipe;
import org.apache.aries.blueprint.utils.generics.TypeInference;
import org.junit.Test;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

import static org.junit.Assert.*;

public class BeanRecipeTest {
	static class Base {
		public static Object getObject() { return null; }
		public static Object getOne(Object o) { return null; }

		public static Object getMany(Object o, String n, String n2) { return null; }
	}
	
	static class Middle extends Base {
		public static Number getObject() { return null; }
		public static Number getOne(Number n) { return null; }
		public static Number getOne(Object o) { return null; }
		
		public static Object getMany(Object o, String n, Number i) { return null; }
		public static Object getBasic(int n) { return 0; }
	}
	
	static class Top extends Middle {
		public static Integer getObject() { return null; }
		public static Integer getOne(Integer n) { return null; }
		
		public static Object getMany(Object o, String n, Number i) { return null; }
		public static Object getBasic(int n) { return 0; }
	}
	
	static class Unrelated {
		public static String getObject() { return null; }
		public static Object getBasic(int n) { return 1; }
	}

    static public interface EventMessage<T> {
    }
    static public interface SequentialPolicy<T> {
    }
    static public class DummySequentialPolicy implements SequentialPolicy<Object> {
    }
    static public class MessageDriven {
        public MessageDriven(SequentialPolicy<? super EventMessage<?>> policy) {
        }
    }

    static public interface Example<A> {}
    static public class ExampleImpl implements Example<String> {}
    static public class ExampleService {
        public ExampleService(Example<String> e) {}
    }

    static public interface BaseInterface<T> { }
    static public interface ExtendedInterface<T0, T1> extends BaseInterface<T1> {}
    static public class MyClass implements ExtendedInterface<String, Integer> { }
    static public class MyClass2<T> implements BaseInterface<T> { }
    static public class MyClass3 extends MyClass2<Long> { }
    static public class MyService {
        public MyService(BaseInterface<? extends Number> e) {}
    }


    static public interface A {
        String getA();
        void setA(String a);
    }
    static public interface B extends A {
        String getB();
        void setB(String b);
        void init();
    }
    static public class C implements B {
        String a = "a", b = "b", c = "c";
        public String getA() {
            return a;
        }
        public void setA(String a) {
            this.a = a;
        }
        public String getB() {
            return b;
        }
        public void setB(String b) {
            this.b = b;
        }
        public String getC() {
            return c;
        }
        public void setC(String c) {
            this.c = c;
        }
        public void init() {
        }
    }
    static public class Factory {
        public B create() {
            return new D();
        }
        private class D extends C {
            String d = "d";
            public String getD() {
                return d;
            }
            public void setD(String d) {
                this.d = d;
            }
            public void init() {
            }
        }
    }

    static public class VarArg {
	    public VarArg(String... as) {
        }
    }

    @Test
    public void parameterWithGenerics() throws Exception {
        BlueprintContainerImpl container = new BlueprintContainerImpl(null, null, null, null, null, null, null, null, null, null);
        BeanRecipe recipe = new BeanRecipe("example", container, ExampleService.class, false, false, false);
        recipe.setArguments(Arrays.<Object>asList(new ExampleImpl()));
        recipe.setArgTypes(Arrays.<String>asList((String) null));
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        recipe.create();
    }

    @Test
    public void parameterWithComplexGenerics1() throws Exception {
        BlueprintContainerImpl container = new BlueprintContainerImpl(null, null, null, null, null, null, null, null, null, null);
        BeanRecipe recipe = new BeanRecipe("example", container, MyService.class, false, false, false);
        recipe.setArguments(Arrays.<Object>asList(new MyClass()));
        recipe.setArgTypes(Arrays.<String>asList((String) null));
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        recipe.create();
    }

    @Test
    public void parameterWithComplexGenerics2() throws Exception {
        BlueprintContainerImpl container = new BlueprintContainerImpl(null, null, null, null, null, null, null, null, null, null);
        BeanRecipe recipe = new BeanRecipe("example", container, MyService.class, false, false, false);
        recipe.setArguments(Arrays.<Object>asList(new MyClass3()));
        recipe.setArgTypes(Arrays.<String>asList((String) null));
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        recipe.create();
    }

    @Test
    public void constructorWithGenerics() throws Exception {
        BlueprintContainerImpl container = new BlueprintContainerImpl(null, null, null, null, null, null, null, null, null, null);
        BeanRecipe recipe = new BeanRecipe("example", container, MessageDriven.class, false, false, false);
        recipe.setArguments(Arrays.<Object>asList(new DummySequentialPolicy()));
        recipe.setArgTypes(Arrays.<String>asList((String) null));
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        recipe.create();
    }

    @Test
    public void constructorWithVarArg() throws Exception {
        BlueprintContainerImpl container = new BlueprintContainerImpl(null, null, null, null, null, null, null, null, null, null);
        BeanRecipe recipe = new BeanRecipe("example", container, VarArg.class, false, false, false);
        recipe.setArguments(Arrays.<Object>asList(Arrays.asList("-web")));
        recipe.setArgTypes(Arrays.<String>asList((String) null));
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        recipe.create();
    }

    @Test
	public void parameterLessHiding() throws Exception {
		Set<Method> methods = new HashSet<Method>(
				Arrays.asList(
						Base.class.getMethod("getObject"),
						Middle.class.getMethod("getObject"),
						Top.class.getMethod("getObject"),
						Unrelated.class.getMethod("getObject")
				));
		
		methods = applyStaticHidingRules(methods);

		assertEquals(2, methods.size());
		assertTrue(methods.contains(Top.class.getMethod("getObject")));
		assertTrue(methods.contains(Unrelated.class.getMethod("getObject")));
		assertFalse(methods.contains(Middle.class.getMethod("getObject")));
	}
	
	@Test
	public void parameterDistinction() throws Exception {
		Set<Method> methods = new HashSet<Method>(
				Arrays.asList(
						Base.class.getMethod("getOne", Object.class),
						Middle.class.getMethod("getOne", Number.class),
						Middle.class.getMethod("getOne", Object.class),
						Top.class.getMethod("getOne", Integer.class)
				));
		
		methods = applyStaticHidingRules(methods);
		
		assertEquals(3, methods.size());
		assertFalse(methods.contains(Base.class.getMethod("getOne", Object.class)));
	}
	
	@Test
	public void multiParameterTest() throws Exception {
		Set<Method> methods = new HashSet<Method>(
				Arrays.asList(
						Base.class.getMethod("getMany", Object.class, String.class, String.class),
						Middle.class.getMethod("getMany", Object.class, String.class, Number.class),
						Top.class.getMethod("getMany", Object.class, String.class, Number.class)
				));
		
		methods = applyStaticHidingRules(methods);
		
		assertEquals(2, methods.size());
		assertFalse(methods.contains(Middle.class.getMethod("getMany", Object.class, String.class, Number.class)));
		
	}
	
	@Test
	public void baseTypeHiding() throws Exception {
		Set<Method> methods = new HashSet<Method>(
				Arrays.asList(
						Middle.class.getMethod("getBasic", int.class),
						Top.class.getMethod("getBasic", int.class),
						Unrelated.class.getMethod("getBasic", int.class)
				));
		
		methods = applyStaticHidingRules(methods);
		
		assertEquals(2, methods.size());
		assertFalse(methods.contains(Middle.class.getMethod("getBasic", int.class)));
	}

    @Test
    public void protectedClassAccess() throws Exception {
        BlueprintContainerImpl container = new BlueprintContainerImpl(null, null, null, null, null, null, null, null, null, null);
        BeanRecipe recipe = new BeanRecipe("a", container, null, false, false, false);
        recipe.setFactoryComponent(new PassThroughRecipe("factory", new Factory().create()));
        recipe.setFactoryMethod("getA");
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        assertNotNull(recipe.create());

        recipe = new BeanRecipe("b", container, null, false, false, false);
        recipe.setFactoryComponent(new PassThroughRecipe("factory", new Factory().create()));
        recipe.setFactoryMethod("getB");
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        assertNotNull(recipe.create());

        recipe = new BeanRecipe("c", container, null, false, false, false);
        recipe.setFactoryComponent(new PassThroughRecipe("factory", new Factory().create()));
        recipe.setFactoryMethod("getC");
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        assertNotNull(recipe.create());

        recipe = new BeanRecipe("d", container, null, false, false, false);
        recipe.setFactoryComponent(new PassThroughRecipe("factory", new Factory().create()));
        recipe.setFactoryMethod("getD");
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        try {
            assertNotNull(recipe.create());
            fail("Should have thrown an exception");
        } catch (ComponentDefinitionException e) {
            // ok
        }

        recipe = new BeanRecipe("a", container, null, false, false, false);
        recipe.setFactoryComponent(new PassThroughRecipe("factory", new Factory()));
        recipe.setFactoryMethod("create");
        recipe.setProperty("a", "a");
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        assertNotNull(recipe.create());

        recipe = new BeanRecipe("b", container, null, false, false, false);
        recipe.setFactoryComponent(new PassThroughRecipe("factory", new Factory()));
        recipe.setFactoryMethod("create");
        recipe.setProperty("b", "b");
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        assertNotNull(recipe.create());

        recipe = new BeanRecipe("c", container, null, false, false, false);
        recipe.setFactoryComponent(new PassThroughRecipe("factory", new Factory()));
        recipe.setFactoryMethod("create");
        recipe.setProperty("c", "c");
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        assertNotNull(recipe.create());

        recipe = new BeanRecipe("d", container, null, false, false, false);
        recipe.setFactoryComponent(new PassThroughRecipe("factory", new Factory()));
        recipe.setFactoryMethod("create");
        recipe.setProperty("d", "d");
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        try {
            assertNotNull(recipe.create());
            fail("Should have thrown an exception");
        } catch (ComponentDefinitionException e) {
            // ok
        }

        recipe = new BeanRecipe("a", container, null, false, false, false);
        recipe.setFactoryComponent(new PassThroughRecipe("factory", new Factory()));
        recipe.setFactoryMethod("create");
        recipe.setInitMethod("init");
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        assertNotNull(recipe.create());

    }


    private Set<Method> applyStaticHidingRules(Collection<Method> methods) {
		try {
			Method m = TypeInference.class.getDeclaredMethod("applyStaticHidingRules", Collection.class);
			m.setAccessible(true);
			return new HashSet<Method>((List<Method>) m.invoke(null, methods));
		} catch (Exception e) { 
			throw new RuntimeException(e);
		}
	}
}
