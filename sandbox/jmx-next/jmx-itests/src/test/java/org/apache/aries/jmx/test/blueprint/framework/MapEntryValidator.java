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

import javax.management.openmbean.CompositeData;
import static junit.framework.Assert.*;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;

public class MapEntryValidator extends AbstractCompositeDataValidator {
    
    NonNullObjectValueValidator keyValidator = null;
    
    ObjectValueValidator valueValidator = null;
    
    public MapEntryValidator(){
        super(BlueprintMetadataMBean.MAP_ENTRY_TYPE);
    }
    
    public void setKeyValueValidator(NonNullObjectValueValidator keyValidator, ObjectValueValidator valueValidator){
        this.keyValidator = keyValidator;
        this.valueValidator = valueValidator;        
    }
    
    public void validate(CompositeData target){
        super.validate(target); //do nothing
        assertNotNull("keyValidator can not be null", keyValidator);
        assertNotNull("valueValidator can not be null", valueValidator);
        
        keyValidator.validate(Util.decode((Byte[])target.get(BlueprintMetadataMBean.KEY)));
        valueValidator.validate(Util.decode((Byte[])target.get(BlueprintMetadataMBean.VALUE)));
    }
    
}
