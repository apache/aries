/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.test.referencelistener;

import org.apache.aries.blueprint.annotation.referencelistener.Availability;
import org.apache.aries.blueprint.annotation.referencelistener.Cardinality;
import org.apache.aries.blueprint.annotation.referencelistener.ReferenceListener;
import org.apache.aries.blueprint.plugin.test.ServiceB;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ReferenceListenerProducer {

    @Produces
    @ReferenceListener(cardinality = Cardinality.SINGLE,
            referenceInterface = ServiceB.class,
            componentName = "producer123",
            filter = "(b=123)"
    )
    @Named("referenceListenerToProduceForSingle")
    public ReferenceListenerToProduce single() {
        return null;
    }

    @Produces
    @ReferenceListener(
            referenceInterface = ServiceB.class,
            componentName = "producer456",
            filter = "(b=456)",
            referenceName = "referenceListForProducer",
            bindMethod = "addMe",
            unbindMethod = "removeMe"
    )
    @Named("referenceListenerToProduceForList")
    public ReferenceListenerToProduceWithoutAnnotation list() {
        return null;
    }

    @Produces
    @ReferenceListener(
            referenceInterface = ServiceB.class,
            bindMethod = "addMe",
            unbindMethod = "removeMe",
            availability = Availability.MANDATORY
    )
    @Singleton
    @Named("referenceListenerToProduceWithBindingMethodsByName")
    public ReferenceListenerToProduce listWithDefinedMethods() {
        return null;
    }

}
