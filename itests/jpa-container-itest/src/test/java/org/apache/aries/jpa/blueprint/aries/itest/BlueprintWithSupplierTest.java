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

import java.util.HashMap;
import java.util.Map;

import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.apache.aries.jpa.itest.testbundle.entities.Car;
import org.apache.aries.jpa.itest.testbundle.service.CarService;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

public class BlueprintWithSupplierTest extends AbstractJPAItest {
    
	CarService carService;

	@Test
    public void testEmfAddQuery() throws Exception {
		Map<String,String> filters = new HashMap<String,String>();
		filters.put("type", "supplier");
		carService = getServie(CarService.class, filters);
		
		resolveBundles();
        Car c = new Car();
        c.setColour("Blue");
        c.setNumberPlate("AB11EMF");
        c.setNumberOfSeats(7);
        c.setEngineSize(1900);

        carService.addCar(c);

        Car car2 = carService.getCar("AB11EMF");
        Assert.assertEquals(c.getNumberPlate(), car2.getNumberPlate());
    }

	@Test
    public void testEmAddQuery() throws Exception {
		Map<String,String> filters = new HashMap<String,String>();
		filters.put("type", "em");
		carService = getServie(CarService.class, filters);
		
		resolveBundles();
        Car c = new Car();
        c.setColour("Blue");
        c.setNumberPlate("AB11EM");
        c.setNumberOfSeats(7);
        c.setEngineSize(1900);

        carService.addCar(c);

        Car car2 = carService.getCar("AB11EM");
        Assert.assertEquals(c.getNumberPlate(), car2.getNumberPlate());
    }
	
	@Test
    public void testSupplierAddQuery() throws Exception {
		Map<String,String> filters = new HashMap<String,String>();
		filters.put("type", "supplier");
		carService = getServie(CarService.class, filters);
		
		resolveBundles();
        Car c = new Car();
        c.setColour("Blue");
        c.setNumberPlate("AB11SUPPLIER");
        c.setNumberOfSeats(7);
        c.setEngineSize(1900);

        carService.addCar(c);

        Car car2 = carService.getCar("AB11SUPPLIER");
        Assert.assertEquals(c.getNumberPlate(), car2.getNumberPlate());
    }


	@Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(), //
            ariesJpa20(), //
            hibernate(), //
            derbyDSF(), //
            testBundleBlueprint(),
            //debug()
        };
    }
}
