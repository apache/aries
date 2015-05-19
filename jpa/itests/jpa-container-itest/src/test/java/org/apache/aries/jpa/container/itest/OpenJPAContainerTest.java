/*  Licensed to the Apache Software Foundation (ASF) under one or more
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
package org.apache.aries.jpa.container.itest;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

public class OpenJPAContainerTest extends AbstractJPAItest {

    @Test
    public void findEntityManagerFactory() throws Exception {
        EntityManagerFactory emf = getEMF(TEST_UNIT);
        EntityManager em = emf.createEntityManager();
        em.close();
    }

    @Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(), //
            ariesJpa20(), //
            // Needed for the BP_TEST_UNIT
            transactionWrapper(), //
            openJpa(), //
            testDs(), //
            testBundle()
        };

    }

}
