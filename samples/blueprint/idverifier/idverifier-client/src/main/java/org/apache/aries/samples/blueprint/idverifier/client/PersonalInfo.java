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
public class PersonalInfo {
	private String personid;
	private String area;
	private String birthday;
	private String gender;
	
	public PersonalInfo(String personid, String area, String birth, String suffix){
		this.personid = personid;
		this.area = area;
		this.birthday = birth;
		this.gender = suffix;
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

	/**
	 * @return the area_code
	 */
	public String getArea() {
		return area;
	}

	/**
	 * @param areaCode the area_code to set
	 */
	public void setArea(String areaCode) {
		area = areaCode;
	}

	/**
	 * @return the birth_code
	 */
	public String getBirthday() {
		return birthday;
	}

	/**
	 * @param birthCode the birth_code to set
	 */
	public void setBirthday(String birthCode) {
		birthday = birthCode;
	}

	/**
	 * @return the suffix_code
	 */
	public String getGender() {
		return gender;
	}

	/**
	 * @param suffixCode the suffix_code to set
	 */
	public void setGender(String suffixCode) {
		gender = suffixCode;
	}
	@Override
	public String toString(){
		System.out.println("********Start of Printing Personal Info**********");
		System.out.println("Area: " + this.getArea());
		System.out.println("Birthday: " + this.getBirthday());
		System.out.println("Gender: "+ this.getGender());
		System.out.println("********End of Printing Personal Info************");
		String delimiter = ",";
		StringBuffer sb = new StringBuffer();
		sb.append("PersonInfo [");
		sb.append("personid="+this.getPersonid()+delimiter);
		sb.append("area=" + this.getArea()+ delimiter);
		sb.append("birthday=" + this.getBirthday() + delimiter);
		sb.append("gender="+ this.getGender());
		sb.append("]");
		return sb.toString();
		
	}

}
