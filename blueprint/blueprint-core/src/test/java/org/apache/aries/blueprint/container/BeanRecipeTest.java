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
import org.junit.Test;
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

    static public interface Example<A> {}
    static public class ExampleImpl implements Example<String> {}
    static public class ExampleService {
        public ExampleService(Example<String> e) {}
    }

    @Test
    public void parameterWithGenerics() throws Exception {
        BlueprintContainerImpl container = new BlueprintContainerImpl(null, null, null, null, null, null, null);
        BeanRecipe recipe = new BeanRecipe("example", container, ExampleService.class, false);
        recipe.setArguments(Arrays.<Object>asList(new ExampleImpl()));
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
	
	private Set<Method> applyStaticHidingRules(Collection<Method> methods) {
		try {
			Method m = BeanRecipe.class.getDeclaredMethod("applyStaticHidingRules", Collection.class);
			m.setAccessible(true);
			return new HashSet<Method>((List<Method>) m.invoke(null, methods));
		} catch (Exception e) { 
			throw new RuntimeException(e);
		}
	}
}
