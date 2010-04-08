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
public class CreditRecordStore {
	private Set<String> personidindex;
	private Set<PersonCreditRecords> personrecords;
	
	public CreditRecordStore(Set<CreditRecord> records){		
		init(records);
	}
	
	void init(Set<CreditRecord> records){
		personidindex = new HashSet<String>();
		personrecords = new HashSet<PersonCreditRecords>();
		
		for (CreditRecord arecord : records){
			personidindex.add(arecord.getPersonid());
		}
		
		for (String personid : personidindex){
			personrecords.add(new PersonCreditRecords(personid));
		}
		
		for (CreditRecord arecord : records){
			PersonCreditRecords target = getAPersonRecords(arecord.getPersonid());
			if ( target != null){
				target.add(arecord);
			}
		}
	}
	
	public synchronized boolean add(CreditRecord arecord){
		boolean b = false;
		
		PersonCreditRecords target = getAPersonRecords(arecord.getPersonid());
		if ( target != null){
			b = target.add(arecord);
		} else {
			PersonCreditRecords apersonrecords = new PersonCreditRecords(arecord.getPersonid());
			apersonrecords.add(arecord);
			personrecords.add(apersonrecords);
			personidindex.add(arecord.getPersonid());
			b = true;
		}		
		return b;
		
	}
	
	public synchronized boolean remove(CreditRecord arecord){
		boolean b = false;
		if (personidindex.contains(arecord.getPersonid())) {
			PersonCreditRecords target = getAPersonRecords(arecord.getPersonid());
			b = target.remove(arecord);
			if (target.isEmpty()){
				personidindex.remove(arecord.getPersonid());
				personrecords.remove(target);
			}
		}		
		return b;
	}
	
	
	
	public PersonCreditRecords getAPersonRecords(String personid){
		PersonCreditRecords result = null;
		for (PersonCreditRecords arecord : this.personrecords){
			if (arecord.getPersonid().equals(personid)){
				result = arecord;
				break;
			}
		}
		return result;
	}
	
	

	/**
	 * @return the personidindex
	 */
	public Set<String> getPersonidindex() {
		return personidindex;
	}

	/**
	 * @return the personrecords
	 */
	public Set<PersonCreditRecords> getPersonrecords() {
		return personrecords;
	}		

}
