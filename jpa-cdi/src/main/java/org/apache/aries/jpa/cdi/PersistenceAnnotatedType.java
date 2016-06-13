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

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.jpa.cdi.support.FilterLiteral;
import org.apache.aries.jpa.cdi.support.ForwardingAnnotatedType;
import org.apache.aries.jpa.cdi.support.InjectLiteral;
import org.apache.aries.jpa.cdi.support.ServiceLiteral;
import org.apache.aries.jpa.cdi.support.SimpleBean;
import org.apache.aries.jpa.cdi.support.SyntheticAnnotatedField;
import org.apache.aries.jpa.cdi.support.UniqueIdentifier;
import org.apache.aries.jpa.cdi.support.UniqueIdentifierLitteral;
import org.apache.aries.jpa.template.JpaTemplate;
import org.ops4j.pax.cdi.extension2.api.OsgiExtension;

import static java.util.Collections.unmodifiableSet;

public class PersistenceAnnotatedType<T> extends ForwardingAnnotatedType<T> {

    private final BeanManager manager;
    private final Set<AnnotatedField<? super T>> fields;
    private final List<Bean<?>> beans = new ArrayList<>();

    public PersistenceAnnotatedType(BeanManager manager, AnnotatedType<T> delegate) {
        super(delegate);
        this.manager = manager;
        this.fields = new HashSet<>();
        for (AnnotatedField<? super T> field : delegate.getFields()) {
            if (field.isAnnotationPresent(PersistenceContext.class)) {
                field = decorateContext(field);
            } else if (field.isAnnotationPresent(PersistenceUnit.class)) {
                field = decorateUnit(field);
            }
            this.fields.add(field);
        }
    }

    private boolean hasUnitName(PersistenceContext pc) {
        return !pc.unitName().isEmpty();
    }

    private boolean hasUnitName(PersistenceUnit pu) {
        return !pu.unitName().isEmpty();
    }

    private <X> AnnotatedField<X> decorateContext(AnnotatedField<X> field) {
        final PersistenceContext persistenceContext = field.getAnnotation(PersistenceContext.class);
        final UniqueIdentifier identifier = UniqueIdentifierLitteral.random();

        Set<Annotation> templateQualifiers = new HashSet<>();
        templateQualifiers.add(ServiceLiteral.SERVICE);
        if (hasUnitName(persistenceContext)) {
            templateQualifiers.add(new FilterLiteral("(osgi.unit.name=" + persistenceContext.unitName() + ")"));
        }
        Bean<JpaTemplate> bean = manager.getExtension(OsgiExtension.class)
                .globalDependency(JpaTemplate.class, templateQualifiers);

        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(identifier);
        Bean<EntityManager> b = new SimpleBean<>(EntityManager.class, Dependent.class, Collections.singleton(EntityManager.class), qualifiers, () -> {
            CreationalContext<JpaTemplate> context = manager.createCreationalContext(bean);
            JpaTemplate template = (JpaTemplate) manager.getReference(bean, JpaTemplate.class, context);
            return EntityManagerProducer.create(template);
        });
        beans.add(b);

        Set<Annotation> fieldAnnotations = new HashSet<>();
        fieldAnnotations.add(InjectLiteral.INJECT);
        fieldAnnotations.add(identifier);
        return new SyntheticAnnotatedField<>(field, fieldAnnotations);
    }

    private <X> AnnotatedField<X> decorateUnit(AnnotatedField<X> field) {
        final PersistenceUnit persistenceUnit = field.getAnnotation(PersistenceUnit.class);
        final UniqueIdentifier identifier = UniqueIdentifierLitteral.random();

        Set<Annotation> templateQualifiers = new HashSet<>();
        templateQualifiers.add(ServiceLiteral.SERVICE);
        if (hasUnitName(persistenceUnit)) {
            templateQualifiers.add(new FilterLiteral("(osgi.unit.name=" + persistenceUnit.unitName() + ")"));
        }
        Bean<EntityManagerFactory> bean = manager.getExtension(OsgiExtension.class)
                .globalDependency(EntityManagerFactory.class, templateQualifiers);

        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(identifier);
        Bean<EntityManagerFactory> b = new SimpleBean<>(EntityManagerFactory.class, Dependent.class, Collections.singleton(EntityManagerFactory.class), qualifiers, () -> {
            CreationalContext<EntityManagerFactory> context = manager.createCreationalContext(bean);
            return (EntityManagerFactory) manager.getReference(bean, EntityManagerFactory.class, context);
        });
        beans.add(b);

        Set<Annotation> fieldAnnotations = new HashSet<>();
        fieldAnnotations.add(InjectLiteral.INJECT);
        fieldAnnotations.add(identifier);
        return new SyntheticAnnotatedField<>(field, fieldAnnotations);
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields() {
        return unmodifiableSet(fields);
    }

    public Collection<? extends Bean<?>> getProducers() {
        return beans;
    }


}
