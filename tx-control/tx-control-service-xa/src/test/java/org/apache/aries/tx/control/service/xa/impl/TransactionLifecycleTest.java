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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.transaction.xa.XAException;

import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.ResourceProvider;

@RunWith(MockitoJUnitRunner.class)
public class TransactionLifecycleTest {

	@Mock
	ResourceProvider<Object> testProvider;
	@Mock
	LocalResource testResource;

	TransactionControlImpl txControl;

	@Before
	public void setUp() throws XAException {
		txControl = new TransactionControlImpl(new GeronimoTransactionManager());
	}

	@Test
	public void testRequired() {

		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			return null;
		});

	}

	@Test
	public void testNestedRequired() {

		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			Object key = txControl.getCurrentContext().getTransactionKey();
			txControl.getCurrentContext().putScopedValue("visible", Boolean.TRUE);

			txControl.required(() -> {
				assertEquals(key, txControl.getCurrentContext().getTransactionKey());
				assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("visible"));
				txControl.getCurrentContext().putScopedValue("visible", Boolean.FALSE);
				return null;
			});

			assertEquals(key, txControl.getCurrentContext().getTransactionKey());
			assertEquals(Boolean.FALSE, txControl.getCurrentContext().getScopedValue("visible"));
			
			return null;
		});

	}

	@Test
	public void testNestedRequiredFromNoTran() {

		txControl.supports(() -> {

			assertFalse(txControl.activeTransaction());

			txControl.getCurrentContext().putScopedValue("invisible", Boolean.TRUE);

			txControl.required(() -> {
				assertTrue(txControl.activeTransaction());
				assertNull(txControl.getCurrentContext().getScopedValue("invisible"));
				txControl.getCurrentContext().putScopedValue("invisible", Boolean.FALSE);
				return null;
			});

			assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("invisible"));
			
			return null;
		});

	}

	@Test
	public void testRequiresNew() {

		txControl.requiresNew(() -> {

			assertTrue(txControl.activeTransaction());

			return null;
		});

	}

	@Test
	public void testNestedRequiresNew() {

		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			Object key = txControl.getCurrentContext().getTransactionKey();
			txControl.getCurrentContext().putScopedValue("invisible", Boolean.TRUE);

			txControl.requiresNew(() -> {
				assertFalse("Parent key " + key + " Child Key " + txControl.getCurrentContext().getTransactionKey(),
						key.equals(txControl.getCurrentContext().getTransactionKey()));
				assertNull(txControl.getCurrentContext().getScopedValue("invisible"));
				txControl.getCurrentContext().putScopedValue("invisible", Boolean.FALSE);
				return null;
			});

			assertEquals(key, txControl.getCurrentContext().getTransactionKey());
			assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("invisible"));
			
			return null;
		});

	}

	@Test
	public void testSupports() {

		txControl.supports(() -> {

			assertFalse(txControl.activeTransaction());

			return null;
		});

	}

	@Test
	public void testNestedSupports() {

		txControl.supports(() -> {

			assertFalse(txControl.activeTransaction());

			txControl.getCurrentContext().putScopedValue("visible", Boolean.TRUE);

			txControl.supports(() -> {
				assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("visible"));
				txControl.getCurrentContext().putScopedValue("visible", Boolean.FALSE);
				return null;
			});
			
			assertEquals(Boolean.FALSE, txControl.getCurrentContext().getScopedValue("visible"));

			return null;
		});

	}

	@Test
	public void testNestedSupportsInActiveTran() {

		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			Object key = txControl.getCurrentContext().getTransactionKey();
			txControl.getCurrentContext().putScopedValue("visible", Boolean.TRUE);

			txControl.supports(() -> {
				assertEquals(key, txControl.getCurrentContext().getTransactionKey());
				assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("visible"));
				txControl.getCurrentContext().putScopedValue("visible", Boolean.FALSE);
				return null;
			});
			
			assertEquals(key, txControl.getCurrentContext().getTransactionKey());
			assertEquals(Boolean.FALSE, txControl.getCurrentContext().getScopedValue("visible"));

			return null;
		});

	}
	
	@Test
	public void testNotSupported() {

		txControl.notSupported(() -> {

			assertFalse(txControl.activeTransaction());

			return null;
		});

	}

	@Test
	public void testNestedNotSupported() {

		txControl.notSupported(() -> {

			assertFalse(txControl.activeTransaction());

			txControl.getCurrentContext().putScopedValue("visible", Boolean.TRUE);

			txControl.notSupported(() -> {
				assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("visible"));
				return null;
			});
			
			assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("visible"));

			return null;
		});

	}

	@Test
	public void testNestedNotSupportedInActiveTran() {

		txControl.required(() -> {

			assertTrue(txControl.activeTransaction());

			Object key = txControl.getCurrentContext().getTransactionKey();
			txControl.getCurrentContext().putScopedValue("invisible", Boolean.TRUE);

			txControl.notSupported(() -> {
				assertFalse(txControl.activeTransaction());
				assertNull(txControl.getCurrentContext().getScopedValue("invisible"));
				txControl.getCurrentContext().putScopedValue("invisible", Boolean.FALSE);
				
				return null;
			});
			
			assertEquals(key, txControl.getCurrentContext().getTransactionKey());
			assertEquals(Boolean.TRUE, txControl.getCurrentContext().getScopedValue("invisible"));

			return null;
		});

	}

}
