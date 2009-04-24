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
package org.apache.geronimo.blueprint.reflect;

import org.osgi.service.blueprint.reflect.RefCollectionMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.apache.geronimo.blueprint.mutable.MutableRefCollectionMetadata;

/**
 * Implementation of RefCollectionMetadata 
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class RefCollectionMetadataImpl extends ServiceReferenceMetadataImpl implements MutableRefCollectionMetadata {

    private Class collectionType;
    private Target comparator;
    private int orderingBasis;
    private int memberType;

    public RefCollectionMetadataImpl() {
    }
    
    public RefCollectionMetadataImpl(RefCollectionMetadata source) {
        super(source);
        collectionType = source.getCollectionType();
        comparator = MetadataUtil.cloneTarget(source.getComparator());
        orderingBasis = source.getOrderingBasis();
        memberType = source.getMemberType();
    }

    public Class getCollectionType() {
        return collectionType;
    }

    public void setCollectionType(Class collectionType) {
        this.collectionType = collectionType;
    }

    public Target getComparator() {
        return comparator;
    }

    public void setComparator(Target comparator) {
        this.comparator = comparator;
    }

    public int getOrderingBasis() {
        return orderingBasis;
    }

    public void setOrderingBasis(int orderingComparisonBasis) {
        this.orderingBasis = orderingComparisonBasis;
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
                "availability=" + availability +
                ", interfaceNames=" + interfaceNames +
                ", componentName='" + componentName + '\'' +
                ", filter='" + filter + '\'' +
                ", serviceListeners=" + serviceListeners +
                ", collectionType=" + collectionType +
                ", comparator=" + comparator +
                ", orderingBasis=" + orderingBasis +
                ", memberType=" + memberType +
                ']';
    }
}
