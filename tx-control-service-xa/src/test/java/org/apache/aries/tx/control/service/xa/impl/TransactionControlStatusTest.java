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
package org.apache.aries.tx.control.service.xa.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.osgi.service.transaction.control.TransactionStatus.ACTIVE;
import static org.osgi.service.transaction.control.TransactionStatus.MARKED_ROLLBACK;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import javax.transaction.xa.XAException;

import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.ResourceProvider;
import org.osgi.service.transaction.control.TransactionContext;

@RunWith(MockitoJUnitRunner.class)
public class TransactionControlStatusTest {

	@Mock
	ResourceProvider<Object> testProvider;
	@Mock
	LocalResource testResource;

	TransactionControlImpl txControl;

	Object resource = new Object();

	@Before
	public void setUp() throws XAException {

		resource = new Object();
		
		txControl = new TransactionControlImpl(new GeronimoTransactionManager());
	}

	@Test
	public void testGetRollbackOnlyUnscoped() {
		try {
			txControl.getRollbackOnly();
			fail("Should not be able to get rollback only");
		} catch (IllegalStateException e) {

		}
	}

	@Test
	public void testSetRollbackOnlyUnscoped() {
		try {
			txControl.setRollbackOnly();
			fail("Should not be able to set rollback only");
		} catch (IllegalStateException e) {

		}
	}

	@Test
	public void testTranChecksUnscoped() {
		assertFalse(txControl.activeTransaction());
		assertFalse(txControl.activeScope());
		assertNull(txControl.getCurrentContext());
	}

	@Test
	public void testGetRollbackOnlyScoped() {
		txControl.notSupported(() -> {
			try {
				txControl.getRollbackOnly();
				fail("Should not be able to get or set rollback when there is no transaction");
			} catch (IllegalStateException ise) {
			}
			return null;
		});
	}

	@Test
	public void testSetRollbackOnlyScoped() {
		txControl.notSupported(() -> {
			try {
				txControl.setRollbackOnly();
				fail("Should not be able to get or set rollback when there is no transaction");
			} catch (IllegalStateException ise) {
			}
			return null;
		});
	}

	@Test
	public void testTranChecksScoped() {
		
		txControl.notSupported(() -> {
			assertFalse(txControl.activeTransaction());
			assertTrue(txControl.activeScope());
			assertNotNull(txControl.getCurrentContext());
			assertEquals(NO_TRANSACTION, txControl.getCurrentContext().getTransactionStatus());
			
			return null;
		});
	}

	@Test
	public void testInheritNotSupported() {
		txControl.notSupported(() -> {
				TransactionContext currentContext = txControl.getCurrentContext();
				return txControl.notSupported(() -> {
						assertSame(currentContext, txControl.getCurrentContext());
						return null;
					});
			});
	}

	@Test
	public void testInheritNotSupportedSupports() {
		txControl.notSupported(() -> {
			TransactionContext currentContext = txControl.getCurrentContext();
			return txControl.supports(() -> {
				assertSame(currentContext, txControl.getCurrentContext());
				return null;
			});
		});
	}

	@Test
	public void testInheritNotSupportedRequired() {
		txControl.notSupported(() -> {
			TransactionContext currentContext = txControl.getCurrentContext();
			txControl.required(() -> {
				assertNotSame(currentContext, txControl.getCurrentContext());
				return null;
			});
			assertSame(currentContext, txControl.getCurrentContext());
			return null;
		});
	}

	@Test
	public void testGetRollbackOnlyActive() {
		txControl.required(() -> {
			assertFalse(txControl.getRollbackOnly());
			return null;
		});
	}
	
	@Test
	public void testSetRollbackOnlyActive() {
		txControl.required(() -> {
			assertFalse(txControl.getRollbackOnly());
			txControl.setRollbackOnly();
			assertTrue(txControl.getRollbackOnly());
			return null;
		});
	}
	
	@Test
	public void testTranChecksActive() {
		
		txControl.required(() -> {
			assertTrue(txControl.activeTransaction());
			assertTrue(txControl.activeScope());
			assertNotNull(txControl.getCurrentContext());
			assertEquals(ACTIVE, txControl.getCurrentContext().getTransactionStatus());

			txControl.setRollbackOnly();
			assertEquals(MARKED_ROLLBACK, txControl.getCurrentContext().getTransactionStatus());
			
			return null;
		});
	}
	
	@Test
	public void testInheritSupports() {
		txControl.required(() -> {
				TransactionContext currentContext = txControl.getCurrentContext();
				return txControl.supports(() -> {
						assertSame(currentContext, txControl.getCurrentContext());
						return null;
					});
			});
	}

	@Test
	public void testInheritRequired() {
		txControl.required(() -> {
			TransactionContext currentContext = txControl.getCurrentContext();
			return txControl.required(() -> {
				assertSame(currentContext, txControl.getCurrentContext());
				return null;
			});
		});
	}

	@Test
	public void testInheritRequiredNotSupported() {
		txControl.required(() -> {
			TransactionContext currentContext = txControl.getCurrentContext();
			txControl.notSupported(() -> {
				assertNotSame(currentContext, txControl.getCurrentContext());
				return null;
			});
			assertSame(currentContext, txControl.getCurrentContext());
			return null;
		});
	}
	
}
