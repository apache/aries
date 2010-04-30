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

import java.util.HashSet;
import java.util.Set;

/**
 * @author forrestxm
 *
 */
public class PersonCreditRecords {
	private String personid;
	private Set<String> recordNOs;
	private Set<CreditRecord> records;
	
	public PersonCreditRecords(String personid){
		this.personid = personid;
		this.recordNOs = new HashSet<String>();
		this.records = new HashSet<CreditRecord>();
	}
	
	public boolean add(CreditRecord arecord){
		boolean b = false;
		if (arecord.getPersonid().equals(personid)){
			if (!recordNOs.contains(arecord.getRecordNO())){
				this.recordNOs.add(arecord.getRecordNO());
				b = this.records.add(arecord);
			}
		}
		return b;
	}
	
	public boolean remove(CreditRecord arecord){
		boolean b = false;
		if (arecord.getPersonid().equals(this.personid)){
			if (recordNOs.contains(arecord.getRecordNO())){
				this.recordNOs.remove(arecord.getRecordNO());				
				b = this.records.remove(getARecord(arecord.getRecordNO()));
			}
		}
		return b;
	}
	
	private CreditRecord getARecord(String recordNO){
		CreditRecord target = null;
		for (CreditRecord arecord : getRecords()){
			if (arecord.getRecordNO().equals(recordNO)){
				target = arecord;
				break;
			}
		}
		
		return target;
	}
	
	public boolean isEmpty(){
		boolean b = false;
		b = recordNOs.isEmpty() && records.isEmpty();		
		return b;
	}

	/**
	 * @return the personid
	 */
	public String getPersonid() {
		return personid;
	}	

	/**
	 * @return the recordNOs
	 */
	public Set<String> getRecordNOs() {
		return recordNOs;
	}	

	/**
	 * @return the records
	 */
	public Set<CreditRecord> getRecords() {
		return records;
	}
	

}
