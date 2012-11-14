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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;


public class BeanValidator extends AbstractCompositeDataValidator implements NonNullObjectValueValidator, TargetValidator{
    
    private boolean validateArgumentsFlag = true;
    private List<BeanArgumentValidator> beanArgumentValidators = new ArrayList<BeanArgumentValidator>();
    private boolean validatePropertiesFlag = true;
    private List<BeanPropertyValidator> beanPropertyValidators = new ArrayList<BeanPropertyValidator>();
    
    public BeanValidator(String className){
        super(BlueprintMetadataMBean.BEAN_METADATA_TYPE);
        setExpectValue(BlueprintMetadataMBean.CLASS_NAME, className);
    }
    
    public BeanValidator(String className, String initMethod){
        super(BlueprintMetadataMBean.BEAN_METADATA_TYPE);
        setExpectValue(BlueprintMetadataMBean.CLASS_NAME, className);
        setExpectValue(BlueprintMetadataMBean.INIT_METHOD, initMethod);
    }
    
    public BeanValidator(String className, String initMethod, String destroyMethod){
        super(BlueprintMetadataMBean.BEAN_METADATA_TYPE);
        setExpectValue(BlueprintMetadataMBean.CLASS_NAME, className);
        setExpectValue(BlueprintMetadataMBean.INIT_METHOD, initMethod);
        setExpectValue(BlueprintMetadataMBean.DESTROY_METHOD, destroyMethod);
    }
    
    public void setValidateArgumentsFlag(boolean flag){
        validateArgumentsFlag = flag;
    }
    
    public void addArgumentValidators(BeanArgumentValidator... validators){
        for (BeanArgumentValidator beanArgumentValidator : validators)
            beanArgumentValidators.add(beanArgumentValidator);
    }
    
    public void setValidatePropertiesFlag(boolean flag){
        validatePropertiesFlag = flag;
    }
            
    public void addPropertyValidators(BeanPropertyValidator... validators){
        for (BeanPropertyValidator beanPropertyValidator : validators)
            beanPropertyValidators.add(beanPropertyValidator);
    }
    
    public void validate(CompositeData target){
        super.validate(target);
        
        //Validate args
        if (validateArgumentsFlag){
            CompositeData[] args = (CompositeData[])target.get(BlueprintMetadataMBean.ARGUMENTS);  
            assertNotNull(args); // at least CompositeData[0]
            assertEquals("The size of arguments is not equals, expect " + beanArgumentValidators.size() + " but got " + args.length, 
                    beanArgumentValidators.size(), args.length);
            for (int i=0; i<beanArgumentValidators.size(); i++) // the order of the arg validators should be the same with the args
                beanArgumentValidators.get(i).validate(args[i]);
        }
        
        //Validate props
        if (validatePropertiesFlag){
            CompositeData[] props = (CompositeData[])target.get(BlueprintMetadataMBean.PROPERTIES);
            assertNotNull(props);
            assertEquals("The size of properties is not equals, expect " + beanPropertyValidators.size() + " but got " + props.length, 
                    beanPropertyValidators.size(), props.length);
            for (int i=0; i<beanPropertyValidators.size(); i++)
                beanPropertyValidators.get(i).validate(props[i]);
            
        }
        
    }
    
}