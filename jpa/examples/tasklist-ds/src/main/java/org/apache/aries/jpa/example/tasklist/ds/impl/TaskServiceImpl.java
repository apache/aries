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
package org.apache.aries.jpa.example.tasklist.ds.impl;

import java.util.Collection;

import javax.persistence.EntityManager;

import org.apache.aries.jpa.example.tasklist.model.Task;
import org.apache.aries.jpa.example.tasklist.model.TaskService;
import org.apache.aries.jpa.template.EmConsumer;
import org.apache.aries.jpa.template.EmFunction;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class TaskServiceImpl implements TaskService {

    private JpaTemplate jpa;

    @Override
    public Task getTask(final Integer id) {
        return jpa.txExpr(TransactionType.Required, new EmFunction<Task>() {
            @Override
            public Task apply(EntityManager em) {
                return em.find(Task.class, id);
            }
        });
    }

    @Override
    public void addTask(final Task task) {
        jpa.tx(new EmConsumer() {
            @Override
            public void accept(EntityManager em) {
                    em.persist(task);
                    em.flush();
            }
        });
    }

    @Override
    public Collection<Task> getTasks() {
        return jpa.txExpr(new EmFunction<Collection<Task>>() {
            @Override
            public Collection<Task> apply(EntityManager em) {
                return em.createQuery("select t from Task t", Task.class).getResultList();
            }
        });
    }

    @Override
    public void updateTask(final Task task) {
        jpa.tx(new EmConsumer() {
            @Override
            public void accept(EntityManager em) {
                em.persist(task);
            }
        });
    }

    @Override
    public void deleteTask(final Integer id) {
        jpa.tx(new EmConsumer() {
            @Override
            public void accept(EntityManager em) {
                em.remove(getTask(id));
            }
        });
    }

    @Reference(target = "(osgi.unit.name=tasklist)")
    public void setJpaTemplate(JpaTemplate jpa) {
        this.jpa = jpa;
    }

    // See below for the Java 8 version with closures
    /*
    public Task getTask(Integer id) {
        return jpa.txExpr(TransactionType.Required, em -> em.find(Task.class, id));
    }

    public void addTask(Task task) {
        jpa.tx(em -> {
            em.persist(task);
            em.flush();
        });
    }

    public Collection<Task> getTasks() {
        return jpa.txExpr(em -> em.createQuery("select t from Task t", Task.class).getResultList());
    }

    public void updateTask(Task task) {
        jpa.tx(em -> em.persist(task));
    }

    public void deleteTask(Integer id) {
        jpa.tx(em -> em.remove(getTask(id)));
    }

    @Reference(target = "(osgi.unit.name=tasklist)")
    public void setJpaTemplate(JpaTemplate jpa) {
        this.jpa = jpa;
    }
    */
}
