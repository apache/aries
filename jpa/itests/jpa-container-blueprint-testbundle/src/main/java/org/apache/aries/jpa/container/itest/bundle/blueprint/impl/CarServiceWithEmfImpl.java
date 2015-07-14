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
package org.apache.aries.jpa.container.itest.bundle.blueprint.impl;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.container.itest.entities.CarService;

public class CarServiceWithEmfImpl implements CarService {

    @PersistenceUnit(unitName = "test_unit_blueprint")
    EntityManagerFactory emf;

    @Override
    public Car getCar(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Car.class, id);
        } finally {
            em.close();
        }
    }

    @Override
    public void addCar(Car car) {
        EntityManager em = emf.createEntityManager();
        em.persist(car);
        em.flush();
        em.close();
    }

    public Collection<Car> getCars() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            return em.createQuery("select c from Car c", Car.class).getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public void updateCar(Car car) {
        EntityManager em = emf.createEntityManager();
        em.persist(car);
        em.close();
    }

    @Override
    public void deleteCar(String id) {
        EntityManager em = emf.createEntityManager();
        em.remove(em.find(Car.class, id));
        em.close();
    }

    public void setEmf(EntityManagerFactory emf) {
        this.emf = emf;
    }

}
