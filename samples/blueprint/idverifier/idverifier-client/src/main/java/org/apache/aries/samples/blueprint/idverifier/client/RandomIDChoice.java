/**
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.blueprint.idverifier.client;

import java.util.Random;

/**
 * @author forrestxm
 *
 */
public class RandomIDChoice {
	private static String[] idarray = {
		"310115197011076874",
		"310115197011277844",
		"110108197710016853",
		"11010819541001366X"
	};
	
	public RandomIDChoice(){
		super();
	}
	
	public String getRandomID(){
		Random randomintgenerator = new Random();
		int randomint = randomintgenerator.nextInt(1000);
		int remain = randomint % idarray.length;
		if (remain < 0 || remain > idarray.length - 1) {
			remain = randomintgenerator.nextInt(1000) % idarray.length;
		}
		return idarray[remain];
	}

}
