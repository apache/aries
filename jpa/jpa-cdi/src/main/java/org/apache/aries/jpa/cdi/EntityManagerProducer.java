/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.jpa.cdi;

import javax.persistence.EntityManager;
import java.lang.reflect.Proxy;

import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;

public class EntityManagerProducer {

    public static EntityManager create(JpaTemplate template) {
        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    try {
                        return template.txExpr(TransactionType.Supports, em -> {
                            try {
                                return method.invoke(em, args);
                            } catch (RuntimeException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (RuntimeException e) {
                        if (e.getClass() == RuntimeException.class
                                && e.getCause() != null
                                && e.getCause().toString().equals(e.getMessage())) {
                            throw e.getCause();
                        }
                        throw e;
                    }
                });
    }

}
