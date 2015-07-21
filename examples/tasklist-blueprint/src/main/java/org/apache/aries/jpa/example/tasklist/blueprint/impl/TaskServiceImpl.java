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
package org.apache.aries.jpa.example.tasklist.blueprint.impl;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.aries.jpa.example.tasklist.model.Task;
import org.apache.aries.jpa.example.tasklist.model.TaskService;
import org.apache.aries.transaction.annotations.Transaction;

public class TaskServiceImpl implements TaskService {

    @PersistenceContext(unitName = "tasklist")
    EntityManager em;

    @Override
    public Task getTask(Integer id) {
        return em.find(Task.class, id);
    }

    @Transaction
    @Override
    public void addTask(Task task) {
        em.persist(task);
        em.flush();
    }

    public Collection<Task> getTasks() {
        return em.createQuery("select t from Task t", Task.class).getResultList();
    }

    @Transaction
    @Override
    public void updateTask(Task task) {
        em.persist(task);
    }

    @Transaction
    @Override
    public void deleteTask(Integer id) {
        em.remove(getTask(id));
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }
}
