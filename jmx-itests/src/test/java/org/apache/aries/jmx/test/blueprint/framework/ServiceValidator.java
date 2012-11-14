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

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;

public class ServiceValidator extends AbstractCompositeDataValidator implements NonNullObjectValueValidator {
    // a TargetValidator can be one of BeanValidator, ReferenceValidator, RefValidator
    TargetValidator serviceComponentValidator = null;
    
    List<MapEntryValidator> servicePropertyValidators = new ArrayList<MapEntryValidator>();
    List<RegistrationListenerValidator> registrationListenerValidators = new ArrayList<RegistrationListenerValidator>();
    
    public ServiceValidator(int autoExport){
        super(BlueprintMetadataMBean.SERVICE_METADATA_TYPE);
        this.setExpectValue(BlueprintMetadataMBean.AUTO_EXPORT, autoExport);
    }
    
    public void setServiceComponentValidator(TargetValidator targetValidator){
        this.serviceComponentValidator = targetValidator;
    }
    
    public void addMapEntryValidator(MapEntryValidator... validators){
        for (MapEntryValidator validator : validators)
            this.servicePropertyValidators.add(validator);
    }
        
    public void addRegistrationListenerValidator(RegistrationListenerValidator... validators){
        for (RegistrationListenerValidator validator : validators)
            this.registrationListenerValidators.add(validator);
    }
    
    public void validate(CompositeData target){
        super.validate(target);
        assertNotNull("ServiceValidator must have a TargetValidator for service component", serviceComponentValidator);
        serviceComponentValidator.validate(Util.decode((Byte[])target.get(BlueprintMetadataMBean.SERVICE_COMPONENT)));
        
        if (servicePropertyValidators.size()!=0){
            CompositeData[] serviceProperties = (CompositeData[])target.get(BlueprintMetadataMBean.SERVICE_PROPERTIES);
            if ( servicePropertyValidators.size() != serviceProperties.length )
                fail("The quantity of the service properties is not the same, expect " +servicePropertyValidators.size()+" but got "+ serviceProperties.length);
            for (int i=0; i<servicePropertyValidators.size(); i++)
                servicePropertyValidators.get(i).validate(serviceProperties[i]);
        }
        
        if (registrationListenerValidators.size() != 0){
            CompositeData[] registrationListeners = (CompositeData[])target.get(BlueprintMetadataMBean.REGISTRATION_LISTENERS);
            if ( registrationListenerValidators.size() != registrationListeners.length )
                fail("The quantity of the registration listeners is not the same, expect " +registrationListenerValidators.size()+" but got "+ registrationListeners.length);
            for (int i=0; i<registrationListenerValidators.size(); i++)
                registrationListenerValidators.get(i).validate(registrationListeners[i]);
        }
        
    }
    
}
