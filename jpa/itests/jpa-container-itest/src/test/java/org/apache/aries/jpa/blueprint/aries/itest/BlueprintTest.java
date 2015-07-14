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
package org.apache.aries.jpa.blueprint.aries.itest;

import java.util.Collection;

import javax.inject.Inject;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.container.itest.entities.CarService;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.service.coordinator.Coordinator;

public class BlueprintTest extends AbstractJPAItest {
    @Inject
    Coordinator coordinator;
    
    @Before
    public void deleteCars() {
        CarService carService = getCarService("emf");
        if (carService.getCar(BLUE_CAR_PLATE)!=null) {
            carService.deleteCar(BLUE_CAR_PLATE);
        }
    }

    @Test
    @Ignore
    public void testCoordination() {
        CarService carService = getCarService("em");
        coordinator.begin("jpa", 0);
        carService.addCar(createBlueCar());
        Collection<Car> cars = carService.getCars();
        carService.updateCar(cars.iterator().next());
        carService.deleteCar(BLUE_CAR_PLATE);
        coordinator.pop().end();
        
    }
    
    @Test
    public void testInjectToMethod() throws Exception {
        carLifecycle(getCarService("method"));
    }

    @Test
    public void testMultiAnnotation() throws Exception {
        carLifecycle(getCarService("multiannotation"));
    }

    @Test
    public void testEmfAddQuery() throws Exception {
        carLifecycle(getCarService("emf"));
    }

    @Test
    public void testEmAddQuery() throws Exception {
        carLifecycle(getCarService("em"));
    }

    @Test
    public void testSupplierAddQuery() throws Exception {
        carLifecycle(getCarService("supplier"));
    }

    private CarService getCarService(String type) {
        return getService(CarService.class, "(type=" + type + ")");
    }

    private void carLifecycle(CarService carService) {
        carService.addCar(createBlueCar());
        assertBlueCar(carService.getCar(BLUE_CAR_PLATE));
    }

    @Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(), //
            ariesJpa20(), //
            hibernate(), //
            derbyDSF(), //
            testBundleBlueprint(),
        // debug()
        };
    }
}
