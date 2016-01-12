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
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;

@Singleton
@Transactional(value=TxType.REQUIRED)
public class MyBean1 extends ParentBean {

    @Autowired
    ServiceA bean2;

    @PersistenceContext(unitName="person")
    EntityManager em;

    @PersistenceUnit(unitName="person")
    EntityManager emf;

    public void overridenInit() {
        // By overriding the method and removing the annotation, this method has lost its
        // @PostConstruct method because it isn't @Inherited
    }

    @PostConstruct
    public void init() {
    }

    public void saveData() {

    }
}
