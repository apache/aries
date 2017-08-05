/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.test.collection;

import org.apache.aries.blueprint.annotation.bean.Bean;
import org.apache.aries.blueprint.annotation.collection.CollectionInject;
import org.apache.aries.blueprint.plugin.AnnotatedService;

import java.util.List;
import java.util.Set;

@Bean
public class BeanWithCollections {

    @CollectionInject(I1.class)
    List<I1> listFieldInject;

    @CollectionInject(I1.class)
    Set<I1> setFieldInject;

    @CollectionInject(I2.class)
    I2[] arrayFieldInject;

    @AnnotatedService
    @CollectionInject(I1.class)
    Set<I1> annotatedSetFieldInject;

    @CollectionInject(I1.class)
    public void setListSetterInject(List<I1> l) {
    }

    @CollectionInject(I2.class)
    public void setSetSetterInject(Set<I1> l) {
    }

    @CollectionInject(I1.class)
    public void setArraySetterInject(I1[] l) {
    }

    @AnnotatedService
    @CollectionInject(I2.class)
    public void setAnnotatedArraySetterInject(I2[] l) {
    }

    public BeanWithCollections(
            @CollectionInject(I1.class) List<I1> listOfI1,
            @CollectionInject(I1.class) Set<I1> setOfI1,
            @CollectionInject(I2.class) I2[] arrayOfI2,
            @CollectionInject(I1.class) @AnnotatedService List<I1> listOfAnnotatedI1,
            @CollectionInject(I3.class) List<I3> listOfNotExistingI3,
            @CollectionInject(I3.class) Set<I3> setOfNotExistingI3,
            @CollectionInject(I3.class) I3[] arrayOfNotExistingI3
    ) {
    }
}
