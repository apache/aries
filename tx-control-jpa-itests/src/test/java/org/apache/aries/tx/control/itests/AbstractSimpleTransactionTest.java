/*
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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.tx.control.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.apache.aries.tx.control.itests.entity.Message;
import org.junit.Test;

public abstract class AbstractSimpleTransactionTest extends AbstractJPATransactionTest {

	@Test
	public void testTx() {
		
		Message message = new Message();
		message.message = "Hello World!";

		txControl.required(() -> {
				em.persist(message);
				return null;
			});

		assertEquals("Hello World!", txControl.notSupported(() -> {
			return em.find(Message.class, message.id).message;
		}));
	}

	@Test
	public void testRollback() {
		
		Message message = new Message();
		message.message = "Hello World!";
		
		txControl.required(() -> {
				em.persist(message);
				txControl.setRollbackOnly();
				return null;
			});
		
		assertEquals(Long.valueOf(0), txControl.notSupported(() -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
			countQuery.select(cb.count(countQuery.from(Message.class)));
			
			return em.createQuery(countQuery).getSingleResult();
		}));
	}

	@Test
	public void testNestedTx() {
		Message message = new Message();
		message.message = "Hello World!";
		
		Message message2 = new Message();
		message2.message = "Hello Nested World!";
		
		txControl.required(() -> {
			em.persist(message);
			
			txControl.requiresNew(() -> {
				em.persist(message2);
				return null;
			});
			
			return null;
		});
		
		List<Message> results = txControl.notSupported(() -> {
				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<Message> query = cb.createQuery(Message.class);
				query.orderBy(cb.asc(query.from(Message.class).get("message")));
				
				return em.createQuery(query).getResultList();
			});
		
		System.out.println(results);
		
		assertEquals(2, results.size());
		assertEquals("Hello Nested World!", results.get(0).message);
		assertEquals("Hello World!", results.get(1).message);
	}
	
	@Test
	public void testNestedTxOuterRollback() {
		Message message = new Message();
		message.message = "Hello World!";
		
		Message message2 = new Message();
		message2.message = "Hello Nested World!";
		
		txControl.required(() -> {
			// This will not end up in the database
			em.persist(message);
			
			// This should only apply to the current transaction level
			txControl.setRollbackOnly();

			// This nested transaction will commit
			txControl.requiresNew(() -> {
				em.persist(message2);
				return null;
			});
			
			return null;
		});
		
		List<Message> results = txControl.notSupported(() -> {
				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<Message> query = cb.createQuery(Message.class);
				query.orderBy(cb.asc(query.from(Message.class).get("message")));
				
				return em.createQuery(query).getResultList();
			});
		
		System.out.println(results);
		
		assertEquals(1, results.size());
		assertEquals("Hello Nested World!", results.get(0).message);
	}

	@Test
	public void testNestedTxInnerRollback() {
		
		Message message = new Message();
		message.message = "Hello World!";
		
		Message message2 = new Message();
		message2.message = "Hello Nested World!";
		
		txControl.required(() -> {
			// This will end up in the database
			em.persist(message);

			// This nested transaction will not commit
			txControl.requiresNew(() -> {
				em.persist(message2);
				txControl.setRollbackOnly();
				return null;
			});
			
			return null;
		});
		
		List<Message> results = txControl.notSupported(() -> {
				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<Message> query = cb.createQuery(Message.class);
				query.orderBy(cb.asc(query.from(Message.class).get("message")));
				
				return em.createQuery(query).getResultList();
			});
		
		System.out.println(results);
		
		assertEquals(1, results.size());
		assertEquals("Hello World!", results.get(0).message);
	}
	
	@Test
	public void testRequiredInheritsTx() {
		
		Message message = new Message();
		message.message = "Hello World!";
		
		Message message2 = new Message();
		message2.message = "Hello Nested World!";
		
		txControl.required(() -> {
			em.persist(message);

			txControl.required(() -> {
				em.persist(message2);
				return null;
			});
			
			return null;
		});
		
		List<Message> results = txControl.notSupported(() -> {
				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<Message> query = cb.createQuery(Message.class);
				query.orderBy(cb.asc(query.from(Message.class).get("message")));
				
				return em.createQuery(query).getResultList();
			});
		
		System.out.println(results);
		
		assertEquals(2, results.size());
		assertEquals("Hello Nested World!", results.get(0).message);
		assertEquals("Hello World!", results.get(1).message);
	}

	@Test
	public void testSuspendedTx() {
		
		Message message = new Message();
		message.message = "Hello World!";

		txControl.required(() -> {
				em.persist(message);
				
				assertEquals(Long.valueOf(0), txControl.notSupported(() -> {
					CriteriaBuilder cb = em.getCriteriaBuilder();
					CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
					countQuery.select(cb.count(countQuery.from(Message.class)));
					
					return em.createQuery(countQuery).getSingleResult();
				}));                
				
				return null;
			});
	}
}
