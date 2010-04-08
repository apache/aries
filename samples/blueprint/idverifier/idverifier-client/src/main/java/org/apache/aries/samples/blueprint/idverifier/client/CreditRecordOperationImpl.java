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

import org.apache.aries.samples.blueprint.idverifier.api.CreditRecordOperation;

/**
 * @author forrestxm
 * 
 */
public class CreditRecordOperationImpl implements CreditRecordOperation {
	private CreditRecordStore recordstore;

	

	public CreditRecordOperationImpl(CreditRecordStore recordstore) {
		super();
		this.recordstore = recordstore;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.aries.blueprint.sample.complex.client.CreditRecordOperation
	 * #add(java.lang.String)
	 */
	public boolean add(String arecord) {
		boolean b = true;
		CreditRecord record = new CreditRecord(arecord);
		b = recordstore.add(record);
		return b;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.aries.blueprint.sample.complex.client.CreditRecordOperation
	 * #query(java.lang.String)
	 */
	public Set<String> query(String personid) {
		Set<String> results = new HashSet<String>();
		
		if (recordstore.getPersonidindex().contains(personid)){
			Set<CreditRecord> allrecords = recordstore.getAPersonRecords(personid).getRecords();
			for (CreditRecord arecord : allrecords){
				results.add(arecord.toString());
			}
		}
		
		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.aries.blueprint.sample.complex.client.CreditRecordOperation
	 * #remove(java.lang.String)
	 */
	public boolean remove(String personid, String recordNO) {
		boolean b = false;

		Set<String> persons = recordstore.getPersonidindex();
		if (persons.contains(personid)) {
			CreditRecord targetproxy = new CreditRecord();
			targetproxy.setPersonid(personid);
			targetproxy.setRecordNO(recordNO);
			b = recordstore.getAPersonRecords(personid).remove(targetproxy);
		}
		
		return b;
	}

}
