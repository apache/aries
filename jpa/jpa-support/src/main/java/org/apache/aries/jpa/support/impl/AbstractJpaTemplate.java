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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.support.impl;

import javax.persistence.EntityManager;

import org.apache.aries.jpa.template.EmConsumer;
import org.apache.aries.jpa.template.EmFunction;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;

public abstract class AbstractJpaTemplate implements JpaTemplate {

    @Override
    public void tx(final TransactionType type, final EmConsumer code) {
        txExpr(type, new EmFunction<Object>() {
            public Object apply(EntityManager em) {
                code.accept(em);
                return null;
            }
        });
    }

    @Override
    public <R> R txExpr(final EmFunction<R> code) {
        return txExpr(TransactionType.Required, code);
    }

    @Override
    public void tx(final EmConsumer code) {
        tx(TransactionType.Required, code);
    }


    protected RuntimeException wrapThrowable(Throwable ex) {
        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        }
        return new RuntimeException(ex);
    }
}
