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
import org.apache.aries.blueprint.annotation.service.MemberType;
import org.apache.aries.blueprint.annotation.service.ReferenceList;
import org.osgi.framework.ServiceReference;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class BeanWithReferenceLists {

    @Inject
    @ReferenceList(referenceInterface = Ref1.class)
    List<Ref1> ref1Field;

    @Inject
    @ReferenceList(referenceInterface = Ref1.class)
    @Named("myRef1List")
    List<Ref1> myRef1Field;

    @Inject
    @ReferenceList(referenceInterface = Ref1.class, filter = "(a=453)", componentName = "r1", availability = Availability.OPTIONAL)
    List<Ref1> myRef1FieldAllProps;

    @Inject
    @ReferenceList(referenceInterface = Ref1.class, filter = "(x=1)", memberType = MemberType.SERVICE_REFERENCE)
    List<ServiceReference<Ref1>> myRef1FieldFilter;

    @Inject
    @ReferenceList(referenceInterface = Ref2.class)
    public void setRef2Setter(List<Ref2> ref) {
    }

    @Inject
    @ReferenceList(referenceInterface = Ref2.class)
    @Named("myRef2List")
    public void setRef2SetterNamed(List<Ref2> ref) {
    }

    @Inject
    @ReferenceList(referenceInterface = Ref2.class, filter = "(b=453)", componentName = "r2", availability = Availability.OPTIONAL)
    public void setRef2SetterFull(List<Ref2> ref) {
    }

    @Inject
    @ReferenceList(referenceInterface = Ref2.class, componentName = "blablabla", memberType = MemberType.SERVICE_REFERENCE)
    public void setRef2SetterComponent(List<ServiceReference<Ref2>> ref) {
    }

    public BeanWithReferenceLists(
            @ReferenceList(referenceInterface = Ref1.class) List<Ref1> ref1,
            @ReferenceList(referenceInterface = Ref2.class, availability = Availability.OPTIONAL, memberType = MemberType.SERVICE_REFERENCE) List<ServiceReference<Ref2>> ref2,
            @ReferenceList(referenceInterface = Ref1.class, filter = "(y=3)") List<Ref1> ref1x,
            @ReferenceList(referenceInterface = Ref1.class, componentName = "compForConstr") List<Ref1> ref1c,
            @ReferenceList(referenceInterface = Ref1.class, filter = "(y=3)", componentName = "compForConstr") List<Ref1> ref1fc,
            @ReferenceList(referenceInterface = Ref1.class, availability = Availability.OPTIONAL) @Named("ref1ListForCons") List<Ref1> ref1Named
    ) {
    }

    @Produces
    @Named("producedWithReferenceLists")
    public String create(
            @ReferenceList(referenceInterface = Ref3.class) List<Ref3> ref3,
            @ReferenceList(referenceInterface = Ref4.class, availability = Availability.OPTIONAL) List<Ref4> ref4a,
            @ReferenceList(referenceInterface = Ref3.class, filter = "(y=3)") List<Ref3> ref3f,
            @ReferenceList(referenceInterface = Ref3.class, componentName = "compForProduces") List<Ref3> ref3c,
            @ReferenceList(referenceInterface = Ref3.class, filter = "(y=3)", componentName = "compForProduces", memberType = MemberType.SERVICE_REFERENCE) List<Ref3> ref3fc,
            @ReferenceList(referenceInterface = Ref3.class) @Named("ref3ListForProduces") List<Ref3> ref3Named
    ) {
        return null;
    }


}
