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
import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;

import org.apache.aries.blueprint.plugin.test.interfaces.ServiceA;
import org.springframework.beans.factory.annotation.Autowired;

public class ParentBean {

    @Autowired
    ServiceA bean2;

    @PersistenceContext(unitName="person")
    EntityManager em;

    @PersistenceUnit(unitName="person")
    EntityManager emf;

    @PostConstruct
    public void overridenInit() {
    }

    @PreDestroy
    public void destroy() {
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void txOverridenWithoutTransactional() {
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public void txSupports() {
    }

    @Transactional(Transactional.TxType.NEVER)
    public void txOverridenWithRequiresNew() {
    }
}
