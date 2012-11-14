/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jmx.test.blueprint.framework;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;

abstract class AbstractServiceReferenceValidator extends AbstractCompositeDataValidator implements NonNullObjectValueValidator {
    private List<ReferenceListenerValidator> referenceListenerValidators = new ArrayList<ReferenceListenerValidator>();
    
    protected AbstractServiceReferenceValidator(CompositeType type) {
        super(type);
    }
    
    public void addReferenceListenerValidator(ReferenceListenerValidator... validators){
        for (ReferenceListenerValidator validator : validators)
            this.referenceListenerValidators.add(validator);
    }
    
    public void validate(CompositeData target){
        super.validate(target);
        if (referenceListenerValidators.size() != 0){
            CompositeData[] referenceListeners = (CompositeData[])target.get(BlueprintMetadataMBean.REFERENCE_LISTENERS);
            if ( referenceListenerValidators.size() != referenceListeners.length )
                fail("The quantity of the listeners is not the same, expect " +referenceListenerValidators.size()+" but got "+ referenceListeners.length);
            for (int i=0; i<referenceListenerValidators.size(); i++)
                referenceListenerValidators.get(i).validate(referenceListeners[i]);
        }
    }
    
}
