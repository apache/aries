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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.container.itest.entities.CarService;
import org.apache.aries.jpa.itest.AbstractCarJPAITest;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;

public class BlueprintTest extends AbstractCarJPAITest {
    @Inject
    Coordinator coordinator;
    
    @Test
    public void testCoordination() {
        assertNoCoordination();
        CarService carService = getCarService("em");
        assertNoCars(carService);
        for (int c=0; c<100; c++) {
            System.out.println(c);
            Coordination coord = null;
            try {
                coord = coordinator.begin("testCoordination", 0);
                carService.addCar(createBlueCar());
                Collection<Car> cars = carService.getCars();
                carService.updateCar(cars.iterator().next());
            } finally {
                coord.end();
            }
            // TODO For some reason I need a second coordination here
            try {
                coord = coordinator.begin("testCoordination", 0);
                carService.deleteCar(BLUE_CAR_PLATE);
                Assert.assertEquals(0, carService.getCars().size());
            } finally {
                coord.end();
            }
        }
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
    public void testEmf() throws Exception {
        carLifecycle(getCarService("emf"));
    }

    @Test
    public void testEm() throws Exception {
        carLifecycle(getCarService("em"));
    }
    
    @Test
    public void testEmJtaAnn() throws Exception {
        carLifecycle(getCarService("emJtaAnn"));
    }

    @Test
    public void testSupplier() throws Exception {
        carLifecycle(getCarService("supplier"));
    }
    
    @Test
    public void testCoordinationLifecycle() throws InterruptedException, ExecutionException {
        CarService carService = getCarService("em");
        assertNoCars(carService);
        Runnable carLifeCycle = getService(Runnable.class, "(type=carCoordinated)");
        carLifeCycle.run();
        ExecutorService exec = Executors.newFixedThreadPool(20);
        List<Future<?>> futures = new ArrayList<>();
        for (int c=0; c<100; c++) {
            futures.add(exec.submit(carLifeCycle));
        }
        exec.shutdown();
        exec.awaitTermination(30, TimeUnit.SECONDS);
        for (Future<?> future : futures) {
            future.get();
        }
        assertNoCars(carService);
    }

    private CarService getCarService(String type) {
        return getService(CarService.class, "(type=" + type + ")");
    }

    private void carLifecycle(CarService carService) {
        assertNoCoordination();
        if (carService.getCar(BLACK_CAR_PLATE) != null) {
            carService.deleteCar(BLUE_CAR_PLATE);
        }
        carService.addCar(createBlueCar());
        assertBlueCar(carService.getCar(BLUE_CAR_PLATE));
        carService.deleteCar(BLUE_CAR_PLATE);
    }

    private void assertNoCoordination() {
        Coordination coord = coordinator.peek();
        Assert.assertNull("There should not be a coordination on this thread", coord);
    }
    
    private void assertNoCars(CarService carService) {
        Assert.assertEquals("Invalid number of cars", 0, carService.getCars().size());
    }

    @Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(), //
            jta12Bundles(), //
            ariesJpa20(), //
            hibernate(), //
            derbyDSF(), //
            testBundle(), //
            testBundleBlueprint(),
        // debug()
        };
    }
}
