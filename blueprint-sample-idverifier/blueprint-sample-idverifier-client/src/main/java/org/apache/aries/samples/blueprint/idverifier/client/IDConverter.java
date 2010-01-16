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

import org.apache.aries.samples.blueprint.idverifier.api.*;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * @author forrestxm
 * 
 */
public class IDConverter implements Converter {
	
	private PersonIDVerifier verifier;
	private String personid;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.service.blueprint.container.Converter#canConvert(java.lang.Object
	 * , org.osgi.service.blueprint.container.ReifiedType)
	 */
	// @Override
	public boolean canConvert(Object sourceObject, ReifiedType targetType) {
		boolean canorcannot = false;
		String id = null;
		if (targetType.getRawClass() == PersonalInfo.class) {
			if (sourceObject instanceof RandomIDChoice){
				id = ((RandomIDChoice)sourceObject).getRandomID();
				this.setPersonid(id);
			}
			//String personid = sourceObject.toString();
			if (this.getPersonid() == null || this.getPersonid().length() != 18) return false;
			verifier.setId(this.getPersonid());
			canorcannot = this.verifier.verify();
		}
		return canorcannot;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.service.blueprint.container.Converter#convert(java.lang.Object,
	 * org.osgi.service.blueprint.container.ReifiedType)
	 */
	// @Override
	public Object convert(Object sourceObject, ReifiedType targetType)
			throws Exception {
		return new PersonalInfo(this.getPersonid(), verifier.getArea(), verifier.getBirthday(), verifier.getGender());
	}	
	
	/**
	 * @return the verifier
	 */
	public PersonIDVerifier getVerifier() {
		return verifier;
	}

	/**
	 * @param verifier
	 *            the verifier to set
	 */
	public void setVerifier(PersonIDVerifier verifier) {
		this.verifier = verifier;
	}

	/**
	 * @return the personid
	 */
	public String getPersonid() {
		return personid;
	}

	/**
	 * @param personid the personid to set
	 */
	public void setPersonid(String personid) {
		this.personid = personid;
	}


}
