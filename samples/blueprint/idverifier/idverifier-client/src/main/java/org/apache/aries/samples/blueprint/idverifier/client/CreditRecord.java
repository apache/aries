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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author forrestxm
 * 
 */
public class CreditRecord {
	private String personid;
	private String recordNO;
	private Date happenedwhen;
	private String recordjustification;
	private String recorddescription;
	
	public CreditRecord(){
		
	}
	
	public CreditRecord(String s){
		this(s, ":");
	}

	public CreditRecord(String s, String delimiter) {
		convert(s, delimiter);
	}

	private void convert(String s, String delimiter) {
		String[] pieces = s.split(delimiter);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if (pieces.length == 5) {
			this.setPersonid(pieces[0]);
			this.setRecordNO(pieces[1]);
			try {
				this.setHappenedwhen(sdf.parse(pieces[2]));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			this.setRecordjustification(pieces[3]);
			this.setRecorddescription(pieces[4]);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CreditRecord [personid=" + personid + ", recordNO=" + recordNO
				+ ", recordjustification=" + recordjustification
				+ ", happenedwhen=" + happenedwhen + ", recorddescription="
				+ recorddescription + "]";
	}

	/**
	 * @return the personid
	 */
	public String getPersonid() {
		return personid;
	}

	/**
	 * @param personid
	 *            the personid to set
	 */
	public void setPersonid(String personid) {
		this.personid = personid;
	}

	/**
	 * @return the recordNO
	 */
	public String getRecordNO() {
		return recordNO;
	}

	/**
	 * @param recordNO
	 *            the recordNO to set
	 */
	public void setRecordNO(String recordNO) {
		this.recordNO = recordNO;
	}

	/**
	 * @return the happenedwhen
	 */
	public Date getHappenedwhen() {
		return happenedwhen;
	}

	/**
	 * @param happenedwhen
	 *            the happenedwhen to set
	 */
	public void setHappenedwhen(Date happenedwhen) {
		this.happenedwhen = happenedwhen;
	}

	/**
	 * @return the recordjustification
	 */
	public String getRecordjustification() {
		return recordjustification;
	}

	/**
	 * @param recordjustification
	 *            the recordjustification to set
	 */
	public void setRecordjustification(String recordjustification) {
		this.recordjustification = recordjustification;
	}

	/**
	 * @return the recorddescription
	 */
	public String getRecorddescription() {
		return recorddescription;
	}

	/**
	 * @param recorddescription
	 *            the recorddescription to set
	 */
	public void setRecorddescription(String recorddescription) {
		this.recorddescription = recorddescription;
	}

}
