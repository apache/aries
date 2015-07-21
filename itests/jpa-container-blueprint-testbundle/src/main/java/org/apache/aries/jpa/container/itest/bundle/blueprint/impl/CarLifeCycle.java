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

import java.util.UUID;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.container.itest.entities.CarService;
import org.osgi.service.coordinator.Coordinator;

/**
 * Programmatically uses a Coordination to do a series of calls with the 
 * same EntityManager
 */
public class CarLifeCycle implements Runnable {
    CarService carService;
    Coordinator coordinator;
    
    @Override
    public void run() {
        Car car = new Car();
        UUID uuid = UUID.randomUUID();
        String id = "blue " + uuid.toString();
        car.setNumberPlate(id);
        carService.addCar(car);
       
//        try {
//            readAndUpdate(id);
//            throw new IllegalStateException("This should not work with an active coordination");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        
        coordinator.begin("jpa", 0);
        readAndUpdate(id);
        coordinator.pop().end();
        
        carService.deleteCar(id);
        Car car2 = carService.getCar(id);
        if (car2 != null) {
            throw new RuntimeException("Car witgh id " + id + " should be deleted");
        }
    }

    /**
     * These operations only work if the EntityManager stays open
     * @param id 
     */
    private void readAndUpdate(String id) {
        Car car = carService.getCar(id);
        car.setEngineSize(100);
        carService.updateCar(car);
    }
    
    public void setCarService(CarService carService) {
        this.carService = carService;
    }
    
    public void setCoordinator(Coordinator coordinator) {
        this.coordinator = coordinator;
    }
}
