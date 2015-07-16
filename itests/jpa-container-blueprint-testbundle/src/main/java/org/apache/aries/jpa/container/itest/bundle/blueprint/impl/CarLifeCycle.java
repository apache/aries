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

import java.util.Collection;

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
        car.setNumberPlate("blue");
        carService.addCar(car);
       
        try {
            readAndUpdate();
            throw new IllegalStateException("This should not work with an active coordination");
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        coordinator.begin("jpa", 0);
        readAndUpdate();
        coordinator.pop().end();
        
        carService.deleteCar("blue");
    }

    /**
     * These operations only work if the EntityManager stays open
     */
    private void readAndUpdate() {
        Collection<Car> cars = carService.getCars();
        carService.updateCar(cars.iterator().next());
    }
    
    public void setCarService(CarService carService) {
        this.carService = carService;
    }
    
    public void setCoordinator(Coordinator coordinator) {
        this.coordinator = coordinator;
    }
}
