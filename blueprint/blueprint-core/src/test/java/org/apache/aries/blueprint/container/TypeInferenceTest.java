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

import org.apache.aries.blueprint.TestBlueprintContainer;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.pojos.DummyServiceTrackerCustomizer;
import org.apache.aries.blueprint.pojos.PojoA;
import org.apache.aries.blueprint.utils.generics.OwbParametrizedTypeImpl;
import org.apache.aries.blueprint.utils.generics.TypeInference;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.util.tracker.ServiceTracker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class TypeInferenceTest {

    private final Converter converter;

    public TypeInferenceTest() throws Exception {
        converter = new AggregateConverter(new TestBlueprintContainer(null));
    }

    @Test
    public void testSimple() {
        TypeInference.Match<Constructor<?>> match = TypeInference.findMatchingConstructors(
                ServiceTracker.class,
                Arrays.asList(
                        new TypeInference.TypedObject(BundleContext.class, null),
                        new TypeInference.TypedObject(new OwbParametrizedTypeImpl(null, Class.class, PojoA.class), PojoA.class),
                        new TypeInference.TypedObject(DummyServiceTrackerCustomizer.class, null)),
                new TIConverter(),
                false)
                .get(0);
        assertNotNull(match);
    }

    @Test
    public void testReorder() {
        TypeInference.Match<Constructor<?>> match = TypeInference.findMatchingConstructors(
                ServiceTracker.class,
                Arrays.asList(
                        new TypeInference.TypedObject(new OwbParametrizedTypeImpl(null, Class.class, PojoA.class), PojoA.class),
                        new TypeInference.TypedObject(BundleContext.class, null),
                        new TypeInference.TypedObject(DummyServiceTrackerCustomizer.class, null)),
                new TIConverter(),
                true)
                .get(0);
        assertNotNull(match);
    }

    @Test
    public void testParameterWithNullCollections() {
        BlueprintContainerImpl container = new BlueprintContainerImpl(null, null, null, null, null, null, null, null, null, null);
        BeanRecipe recipe = new BeanRecipe("sessionDef", container, FactoryWithList.class, false, false, false);
        recipe.setArguments(Collections.singletonList(null));
        recipe.setArgTypes(Collections.<String>singletonList(null));
        recipe.setFactoryMethod("init");
        ExecutionContext.Holder.setContext(new BlueprintRepository(container));
        recipe.create();
    }

    class TIConverter implements TypeInference.Converter {
        public TypeInference.TypedObject convert(TypeInference.TypedObject from, Type to) throws Exception {
            Object arg = from.getValue();
            arg = converter.convert(arg, new GenericType(to));
            return new TypeInference.TypedObject(to, arg);
        }
    }

    public static class FactoryWithList {

        public static String init(List<Integer> ints) {
            return "Hello!";
        }

    }

}
