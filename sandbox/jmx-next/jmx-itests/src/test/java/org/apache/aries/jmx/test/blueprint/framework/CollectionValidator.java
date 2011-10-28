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

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;

public class CollectionValidator extends AbstractCompositeDataValidator implements NonNullObjectValueValidator {
    
    private List<ObjectValueValidator> collectionValueValidators = new ArrayList<ObjectValueValidator>();
    
    public CollectionValidator(String collectionClass){
        super(BlueprintMetadataMBean.COLLECTION_METADATA_TYPE);
        setExpectValue(BlueprintMetadataMBean.COLLECTION_CLASS, collectionClass);
    }
    
    public CollectionValidator(String collectionClass, String valueType){
        super(BlueprintMetadataMBean.COLLECTION_METADATA_TYPE);
        setExpectValue(BlueprintMetadataMBean.COLLECTION_CLASS, collectionClass);
        setExpectValue(BlueprintMetadataMBean.VALUE_TYPE, valueType);
    }
    
    public void addCollectionValueValidators (ObjectValueValidator...objectValueValidators){
        for (ObjectValueValidator objectValueValidator: objectValueValidators)
            collectionValueValidators.add(objectValueValidator);
    }
    
    public void validate(CompositeData target){
        super.validate(target);
        if (collectionValueValidators.size() != 0){
            Byte[][] allWrapValues = (Byte[][])target.get(BlueprintMetadataMBean.VALUES);
            if ( collectionValueValidators.size() != allWrapValues.length )
                fail("The quantity of the values is not the same, expect " +collectionValueValidators.size()+" but got "+ allWrapValues.length);
            for(int i=0;i<collectionValueValidators.size();i++){
                collectionValueValidators.get(i).validate(Util.decode(allWrapValues[i]));
            }
                
        }
    }
}
