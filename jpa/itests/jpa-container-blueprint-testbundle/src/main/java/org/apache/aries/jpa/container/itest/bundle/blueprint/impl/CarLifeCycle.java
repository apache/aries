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
package org.apache.aries.jpa.container.itest.bundle.blueprint.impl;

import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.container.itest.entities.CarService;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;

/**
 * Programmatically uses a Coordination to do a series of calls with the 
 * same EntityManager
 */
public class CarLifeCycle implements Runnable {
    CarService carService;
    Coordinator coordinator;
    
    @Transactional(Transactional.TxType.REQUIRED)
    @Override
    public void run() {
        Car car = new Car();
        UUID uuid = UUID.randomUUID();
        String id = "blue " + uuid.toString();
        car.setEngineSize(1);
        car.setNumberPlate(id);
        carService.addCar(car);
        EntityManager em = getEmFromCoord();
        if (!em.contains(car)) {
            throw new IllegalStateException("Transaction should cause EntityManager to be kept open");
        }
        readAndUpdate(id);
        Car car3 = carService.getCar(id);
        if (car3.getEngineSize() != 100) {
            throw new IllegalStateException("Engine size should have been changed to 100");
        }
        carService.deleteCar(id);
        Car car2 = carService.getCar(id);
        if (car2 != null) {
            throw new RuntimeException("Car with id " + id + " should be deleted"); // NOSONAR
        }
    }

    public void readAndUpdate(String id) {
        Car car = carService.getCar(id);
        if (car == null) {
            throw new IllegalStateException("Expected a car with id " + id);
        }
        car.setEngineSize(100);
    }
    
    @SuppressWarnings("unchecked")
    private EntityManager getEmFromCoord() {
        Coordination coord = coordinator.peek();
        if (coord == null) {
            throw new IllegalStateException("No coordination found");
        }
        while (coord != null) {
            Map<String, EntityManager> emMap = (Map<String, EntityManager>)coord.getVariables().get(EntityManager.class);
            if (emMap != null) {
                return emMap.values().iterator().next();
            }
            coord = coord.getEnclosingCoordination();
        }
        throw new IllegalStateException("No EntityManager found in coordinations");
    }

    public void setCarService(CarService carService) {
        this.carService = carService;
    }
    
    public void setCoordinator(Coordinator coordinator) {
        this.coordinator = coordinator;
    }
}
