/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.osgi.service.blueprint.reflect;

public interface RefCollectionMetadata extends ServiceReferenceMetadata {

    static final int ORDERING_BASIS_SERVICE = 1;

    static final int ORDERING_BASIS_SERVICE_REFERENCE = 2;

    static final int MEMBER_TYPE_SERVICE_INSTANCE = 1;

    static final int MEMBER_TYPE_SERVICE_REFERENCE = 2;

    Class getCollectionType();

    Target getComparator();

    int getOrderingBasis();

    int getMemberType();

}
