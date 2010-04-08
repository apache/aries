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
package org.apache.aries.samples.blueprint.idverifier.server;

/**
 * @author forrestxm
 *
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.aries.samples.blueprint.idverifier.api.PersonIDVerifier;

public class PersonIDVerifierComplexImpl extends PersonIDVerifierSimpleImpl
		implements PersonIDVerifier {
	private String datepattern;
	private Map<String, String> definedAreacode;
	private int[] coefficient;

	@Override
	public boolean verify() {
		boolean b = false;
		b = super.isValidID() && super.isValidSuffix()
				&& this.isValidBirth() && this.isValidArea()
				&& this.isValidCheckCode();
		return b;
	}
	
	@Override
	boolean isValidBirth(){		
		String birthdate = toDateFormat(getBirthcode(), "-");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date birthday = sdf.parse(birthdate);
			SimpleDateFormat sdf_usecustom = new SimpleDateFormat(datepattern);
			this.setBirth_str(sdf_usecustom.format(birthday));
			return true;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return false;		
	}

	@Override
	boolean isValidArea() {
		boolean b = false;
		if (super.isValidArea()	&& definedAreacode.containsValue(getAreacode())) {
			b = true;
			Set<String> keys = definedAreacode.keySet();			
			for (String key:keys){
				String value = definedAreacode.get(key);
				if (value.equals(getAreacode())){
					super.setArea_str(key);
					break;
				}
			}			
		}
		return b;
	}

	public boolean isValidCheckCode() {
		boolean b = false;
		
		int[] codes = this.Char2Number(id.substring(0, 17));
		String[] validcheckcodes = { "1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2" };
		int sum = 0;
		for (int i = 0; i < 17; i++) {
			sum = sum + codes[i] * coefficient[i];
		}

		int remain = sum % 11;
		String checkcode = id.substring(17);		
		
		b = validcheckcodes[remain].equals(checkcode);
		return b;
	}

	private int[] Char2Number(String id) {
		int[] numbers = new int[17];
		for (int i = 0; i < 17; i++) {
			numbers[i] = Integer.parseInt(new Character(id.charAt(i)).toString());
		}
		return numbers;
	}

	/**
	 * @return the datepattern
	 */
	public String getDatepattern() {
		return datepattern;
	}

	/**
	 * @param datepattern
	 *            the datepattern to set
	 */
	public void setDatepattern(String datepattern) {
		this.datepattern = datepattern;
	}	

	/**
	 * @return the definedAreacode
	 */
	public Map<String, String> getDefinedAreacode() {
		return definedAreacode;
	}

	/**
	 * @param definedAreacode
	 *            the definedAreacode to set
	 */
	public void setDefinedAreacode(Map<String, String> definedAreacode) {
		this.definedAreacode = definedAreacode;
	}

	/**
	 * @return the coefficient
	 */
	public int[] getCoefficient() {
		return coefficient;
	}

	/**
	 * @param coefficient
	 *            the coefficient to set
	 */
	public void setCoefficient(int[] coefficient) {
		this.coefficient = coefficient;
	}

}
