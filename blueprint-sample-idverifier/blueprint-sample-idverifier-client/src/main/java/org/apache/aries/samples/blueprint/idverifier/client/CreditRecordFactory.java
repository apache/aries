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

/**
 * @author forrestxm
 * 
 */
public class CreditRecordFactory {
	public static CreditRecord staticCreateBean(String record) {
		staticcount++;
		return new CreditRecord(record);
	}

	private String targetbeanname;
	private static int staticcount = 0;
	private static int dynamiccount = 0;

	public CreditRecordFactory(String beanname) {
		this.targetbeanname = beanname;
	}

	public CreditRecord dynamicCreateBean(String record) {
		dynamiccount++;
		return new CreditRecord(record);
	}

	public void creationStatistics() {
		System.out.println("**********Bean factory "
				+ this.getClass().getSimpleName()
				+ " says goodbye!************");
		System.out.println("**********I created " + staticcount + " "
				+ targetbeanname + " with static factory, " + dynamiccount
				+ " " + targetbeanname + " with dynamic factory.***********");
	}

}
