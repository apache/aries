/*
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
package org.apache.aries.blueprint.reflect;

import org.apache.aries.blueprint.mutable.MutableReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;

/**
 * Implementation of RefCollectionMetadata 
 *
 * @version $Rev$, $Date$
 */
public class ReferenceListMetadataImpl extends ServiceReferenceMetadataImpl implements MutableReferenceListMetadata {

    private int memberType = USE_SERVICE_OBJECT;

    public ReferenceListMetadataImpl() {
    }
    
    public ReferenceListMetadataImpl(ReferenceListMetadata source) {
        super(source);
        memberType = source.getMemberType();
    }

    public int getMemberType() {
        return memberType;
    }

    public void setMemberType(int memberType) {
        this.memberType = memberType;
    }

    @Override
    public String toString() {
        return "RefCollectionMetadata[" +
                "id='" + id + '\'' +
                ", activation=" + activation +
                ", dependsOn=" + dependsOn +
                ", availability=" + availability +
                ", interface='" + interfaceName + '\'' +
                ", componentName='" + componentName + '\'' +
                ", filter='" + filter + '\'' +
                ", referenceListeners=" + referenceListeners +
                ", memberType=" + memberType +
                ']';
    }
}
