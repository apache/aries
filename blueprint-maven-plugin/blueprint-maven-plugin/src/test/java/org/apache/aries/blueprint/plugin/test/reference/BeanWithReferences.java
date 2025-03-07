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
package org.apache.aries.blueprint.plugin.test.reference;

import org.apache.aries.blueprint.annotation.service.Availability;
import org.apache.aries.blueprint.annotation.service.Reference;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class BeanWithReferences {

    @Inject
    @Reference
    Ref1 ref1Field;

    @Inject
    @Reference
    @Named("myRef1")
    Ref1 myRef1Field;

    @Inject
    @Reference(filter = "(a=453)", componentName = "r1", timeout = 2000, availability = Availability.OPTIONAL)
    Ref1 myRef1FieldAllProps;

    @Inject
    @Reference(filter = "(x=1)")
    Ref1 myRef1FieldFilter;

    @Inject
    @Reference
    public void setRef2Setter(Ref2 ref) {
    }

    @Inject
    @Reference
    @Named("myRef2")
    public void setRef2SetterNamed(Ref2 ref) {
    }

    @Inject
    @Reference(filter = "(b=453)", componentName = "r2", timeout = 1000, availability = Availability.OPTIONAL)
    public void setRef2SetterFull(Ref2 ref) {
    }

    @Inject
    @Reference(componentName = "blablabla")
    public void setRef2SetterComponent(Ref2 ref) {
    }

    public BeanWithReferences(
            @Reference Ref1 ref1,
            @Reference(availability = Availability.OPTIONAL, timeout = 20000) Ref2 ref2,
            @Reference(filter = "(y=3)") Ref1 ref1x,
            @Reference(componentName = "compForConstr") Ref1 ref1c,
            @Reference(filter = "(y=3)", componentName = "compForConstr") Ref1 ref1fc,
            @Reference(availability = Availability.OPTIONAL) @Named("ref1ForCons") Ref1 ref1Named
    ) {
    }

    @Produces
    @Named("producedWithReferences")
    public String create(
            @Reference Ref3 ref3,
            @Reference(timeout = 20000) Ref4 ref4,
            @Reference(availability = Availability.OPTIONAL) Ref4 ref4a,
            @Reference(filter = "(y=3)") Ref3 ref3f,
            @Reference(componentName = "compForProduces") Ref3 ref3c,
            @Reference(filter = "(y=3)", componentName = "compForProduces") Ref3 ref3fc,
            @Reference(timeout = 1000) @Named("ref3ForProduces") Ref3 ref3Named
    ) {
        return null;
    }


}
