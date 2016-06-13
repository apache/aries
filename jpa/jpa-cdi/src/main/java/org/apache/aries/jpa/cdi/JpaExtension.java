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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class JpaExtension implements Extension {

    List<Bean<?>> beans = new ArrayList<Bean<?>>();

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> event, BeanManager manager) {
        boolean hasPersistenceField = false;
        for (AnnotatedField<? super T> field : event.getAnnotatedType().getFields()) {
            if (field.isAnnotationPresent(PersistenceContext.class)
                    || field.isAnnotationPresent(PersistenceUnit.class)) {
                hasPersistenceField = true;
                break;
            }
        }
        if (hasPersistenceField) {
            PersistenceAnnotatedType<T> pat = new PersistenceAnnotatedType<T>(manager, event.getAnnotatedType());
            beans.addAll(pat.getProducers());
            event.setAnnotatedType(pat);
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        for (Bean<?> bean : beans) {
            event.addBean(bean);
        }
    }


}
