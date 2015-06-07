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

import javax.persistence.PersistenceContext;

import org.apache.aries.jpa.example.tasklist.model.Task;
import org.apache.aries.jpa.example.tasklist.model.TaskService;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.apache.aries.transaction.annotations.Transaction;


public class TaskServiceWithSupplier implements TaskService {

    @PersistenceContext(unitName = "tasklist")
    EmSupplier em;

    @Override
    public Task getTask(Integer id) {
        return em.get().find(Task.class, id);
    }

    @Transaction
    @Override
    public void addTask(Task task) {
        em.get().persist(task);
        em.get().flush();
    }

    public Collection<Task> getTasks() {
        return em.get().createQuery("select t from Task t", Task.class).getResultList();
    }
    
    @Override
    public void updateTask(Task task) {
        em.get().persist(task);
    }

    @Override
    public void deleteTask(Integer id) {
        em.get().remove(getTask(id));
    }

    public void setEm(EmSupplier em) {
        this.em = em;
    }
    
    public void init() {
        addTask(new Task(1, "Test", "Testdescription"));
    }
}
