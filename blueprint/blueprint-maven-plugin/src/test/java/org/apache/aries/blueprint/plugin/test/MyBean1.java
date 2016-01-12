/**
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
package org.apache.aries.blueprint.plugin.test;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@Singleton
@Transactional(value=TxType.REQUIRES_NEW)
public class MyBean1 extends ParentBean {

    public void overridenInit() {
        // By overriding the method and removing the annotation, this method has lost its
        // @PostConstruct method because it isn't @Inherited
    }

    @PostConstruct
    public void init() {

    }

    @Transactional(TxType.NOT_SUPPORTED)
    public void txNotSupported() {
    }

    @Transactional(TxType.MANDATORY)
    public void txMandatory() {
    }

    @Transactional(TxType.NEVER)
    public void txNever() {
    }

    @Transactional(TxType.REQUIRED)
    public void txRequired() {
    }

    @Override
    public void txOverridenWithoutTransactional() {
    }

    @Transactional(TxType.REQUIRES_NEW)
    public void txOverridenWithRequiresNew() {
    }
}
