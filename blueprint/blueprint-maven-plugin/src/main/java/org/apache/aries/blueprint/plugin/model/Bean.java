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
package org.apache.aries.blueprint.plugin.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

public class Bean extends BeanRef {
    public String initMethod;
    public String destroyMethod;
    public SortedSet<Property> properties;
    public List<Field> persistenceFields;
    public Set<TransactionalDef> transactionDefs = new HashSet<TransactionalDef>();
    public boolean isPrototype;

    public Bean(Class<?> clazz) {
        super(clazz, BeanRef.getBeanName(clazz));
        Introspector introspector = new Introspector(clazz);

        // Init method
        Method initMethod = introspector.methodWith(PostConstruct.class);
        if (initMethod != null) {
            this.initMethod = initMethod.getName();
        }

        // Destroy method
        Method destroyMethod = introspector.methodWith(PreDestroy.class);
        if (destroyMethod != null) {
            this.destroyMethod = destroyMethod.getName();
        }

        // Transactional methods
        transactionDefs.addAll(new JavaxTransactionFactory().create(clazz));
        transactionDefs.addAll(new SpringTransactionFactory().create(clazz));
        this.isPrototype = isPrototype(clazz);
        this.persistenceFields = introspector.fieldsWith(PersistenceContext.class, PersistenceUnit.class);
        properties = new TreeSet<Property>();
    }

    private boolean isPrototype(Class<?> clazz)
    {
        return clazz.getAnnotation(Singleton.class) == null && clazz.getAnnotation(Component.class) == null;
    }

    public void resolve(Matcher matcher) {
        for (Field field : new Introspector(clazz).fieldsWith(Value.class, Autowired.class, Inject.class)) {
            Property prop = Property.create(matcher, field);
            if (prop != null) {
                properties.add(prop);
            }
        }
    }



    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clazz == null) ? 0 : clazz.getName().hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return clazz.getName();
    }

    public void writeProperties(PropertyWriter writer) {
        for (Property property : properties) {
            writer.writeProperty(property);
        }
    }

}
