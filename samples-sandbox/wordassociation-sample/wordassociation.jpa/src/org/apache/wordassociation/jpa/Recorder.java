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

package org.apache.wordassociation.jpa;

import javax.persistence.EntityManager;

import org.apache.wordassociation.AssociationRecorderService;

/**
 * 
 * A JPA-backed implementation of the association recorder service.
 * 
 */
public class Recorder implements AssociationRecorderService {

	private EntityManager em;

	@Override
	public void recordAssociation(String word, String association) {
		Association found = em.find(Association.class, word);
		if (found != null) {
			found.setAssociated(association);
		} else {
			Association a = new Association();
			a.setWord(word);
			a.setAssociated(association);
			em.persist(a);
		}
	}

	@Override
	public String getLastAssociation(String word) {
		Association found = em.find(Association.class, word);
		if (found != null) {
			return found.getAssociated();
		} else {
			return "nothing";
		}
	}

	public void setEntityManager(EntityManager e) {
		em = e;
	}

}
