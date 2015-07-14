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
package org.apache.aries.jpa.itest.testbundle.service.impl;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.aries.jpa.itest.testbundle.entities.Car;
import org.apache.aries.jpa.itest.testbundle.service.CarService;
import org.apache.aries.jpa.supplier.EmSupplier;

public class CarServiceWithMultiAnnotationImpl implements CarService {

    @PersistenceContext(unitName = "test_unit_blueprint")
    EntityManager em;

    @PersistenceUnit(unitName = "test_unit_blueprint")
    EntityManagerFactory emf;

    @PersistenceContext(unitName = "test_unit_blueprint")
    EmSupplier ems;

    @Override
    public Car getCar(String id) {
        return em.find(Car.class, id);
    }

    @Override
    public void addCar(Car car) {
        EntityManager localEm = emf.createEntityManager();
        localEm.persist(car);
        localEm.flush();
        localEm.close();
    }

    public Collection<Car> getCars() {
        return em.createQuery("select c from Car c", Car.class).getResultList();
    }

    @Override
    public void updateCar(Car car) {
        em.persist(car);
    }

    @Override
    public void deleteCar(String id) {
        ems.get().remove(getCar(id));
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }

    public void setEmf(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void setEms(EmSupplier ems) {
        this.ems = ems;
    }
}
