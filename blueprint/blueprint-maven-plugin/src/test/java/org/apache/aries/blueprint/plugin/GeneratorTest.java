/**
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
package org.apache.aries.blueprint.plugin;

import static java.util.Arrays.asList;
import static org.apache.aries.blueprint.plugin.FilteredClassFinder.findClasses;

import java.util.Set;

import org.apache.aries.blueprint.plugin.Generator;
import org.apache.aries.blueprint.plugin.model.Context;
import org.apache.aries.blueprint.plugin.test.MyBean1;
import org.apache.xbean.finder.ClassFinder;
import org.junit.Test;


public class GeneratorTest {
    @Test
    public void testGenerate() throws Exception {
        ClassFinder classFinder = new ClassFinder(this.getClass().getClassLoader());
        String packageName = MyBean1.class.getPackage().getName();
        Set<Class<?>> beanClasses = findClasses(classFinder, asList(packageName));
        Context context = new Context(beanClasses);
        context.resolve();
        new Generator(context, System.out).generate();
    }
    
}
