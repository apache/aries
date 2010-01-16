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
/**
 * @author forrestxm
 *
 */
package org.apache.aries.samples.blueprint.idverifier.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.samples.blueprint.idverifier.api.PersonIDVerifier;

public class PersonIDVerifierSimpleImpl implements PersonIDVerifier {
	String areacode;
	String birthcode;
	String suffixcode;
	
	String id;
	
	String area_str;
	String birth_str;
	String gender_str;
	
	static final String GENDER_MAN = "man";
	static final String GENDER_WOMAN = "woman";
	
	/**
	 * @return the area_str
	 */
	public String getArea_str() {
		return area_str;
	}

	/**
	 * @param areaStr the area_str to set
	 */
	public void setArea_str(String areaStr) {
		area_str = areaStr;
	}

	/**
	 * @return the birth_str
	 */
	public String getBirth_str() {
		return birth_str;
	}

	/**
	 * @param birthStr the birth_str to set
	 */
	public void setBirth_str(String birthStr) {
		birth_str = birthStr;
	}

	/**
	 * @return the gender_str
	 */
	public String getGender_str() {
		return gender_str;
	}

	/**
	 * @param genderStr the gender_str to set
	 */
	public void setGender_str(String genderStr) {
		gender_str = genderStr;
	}

	/**
	 * @return the areacode
	 */
	public String getAreacode() {
		return areacode;
	}

	/**
	 * @param areacode the areacode to set
	 */
	public void setAreacode(String areacode) {
		this.areacode = areacode;
	}

	/**
	 * @return the birthcode
	 */
	public String getBirthcode() {
		return birthcode;
	}

	/**
	 * @param birthcode the birthcode to set
	 */
	public void setBirthcode(String birthcode) {
		this.birthcode = birthcode;
	}

	/**
	 * @return the suffixcode
	 */
	public String getSuffixcode() {
		return suffixcode;
	}

	/**
	 * @param suffixcode the suffixcode to set
	 */
	public void setSuffixcode(String suffixcode) {
		this.suffixcode = suffixcode;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;

	}	

	public String getArea() {
		
		return this.getArea_str();
	}

	public String getBirthday() {
		
		return this.getBirth_str();
	}

	public String getGender() {		
		return this.getGender_str();
	}

	

	public boolean verify() {
		boolean b = false;
		b = isValidID() && isValidArea() && isValidBirth() && isValidSuffix();
		return b;
	}
	
	boolean isValidID(){
		boolean b = false;
		if (this.id.length() == 18) {
			b = true;
			this.setAreacode(this.id.substring(0, 6));
			this.setBirthcode(this.id.substring(6, 14));
			this.setSuffixcode(this.id.substring(14));
		}
		return b;
	}
	
	boolean isValidArea(){
		boolean b = false;
		Pattern p = Pattern.compile("\\d{6}");
		Matcher m = p.matcher(getAreacode());
		if (m.matches()){
			this.setArea_str(getAreacode());
			b = true;
		}
		return b;
	}
	
	boolean isValidBirth() {
		String birthdate = toDateFormat(getBirthcode(), "-");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			this.setBirth_str(sdf.format(sdf.parse(birthdate)));
			return true;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	boolean isValidSuffix() {
		boolean b = false;
		Pattern p = Pattern.compile("\\d{3}[\\dX]");
		Matcher m = p.matcher(getSuffixcode());
		if(m.matches()){
			b = true;
			setGender(getSuffixcode());
		}
		return b;
	}

	String toDateFormat(String s, String delimiter) {
		StringBuffer sb = new StringBuffer();
		sb.append(s.substring(0, 4));
		sb.append(delimiter);
		sb.append(s.substring(4, 6));
		sb.append(delimiter);
		sb.append(s.substring(6));
		return sb.toString();
	}
	
	private void setGender(String s){
		int gender = Integer.parseInt(new Character(s.charAt(2)).toString());
		int remain = gender % 2;
		if (remain == 0 ){
			this.setGender_str(GENDER_WOMAN);
		} else {
			this.setGender_str(GENDER_MAN);
		}
	}

}
