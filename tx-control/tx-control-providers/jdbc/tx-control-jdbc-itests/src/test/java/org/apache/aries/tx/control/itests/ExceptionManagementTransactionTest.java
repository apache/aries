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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionRolledBackException;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ExceptionManagementTransactionTest extends AbstractTransactionTest {
	
	@Test
	public void testRuntimeException() {
		
		RuntimeException toThrow = new RuntimeException("Bang!");
		
		try {
			txControl.required(() -> {
						connection.createStatement()
							.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
						throw toThrow;
					});
			fail("An exception should occur!");
		} catch (ScopedWorkException swe) {
			assertSame(toThrow, swe.getCause());
		}

		assertRollback();
	}

	@Test
	public void testCheckedException() {
		URISyntaxException toThrow = new URISyntaxException("yuck", "Bang!");
		
		try {
			txControl.required(() -> {
						connection.createStatement()
							.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
						throw toThrow;
					});
			fail("An exception should occur!");
			// We have to catch Exception as the compiler complains
			// otherwise
		} catch (ScopedWorkException swe) {
			assertSame(toThrow, swe.getCause());
		}

		assertRollback();
	}

	@Test
	public void testPreCompletionException() {
		RuntimeException toThrow = new RuntimeException("Bang!");
		
		try {
			txControl.required(() -> {
				txControl.getCurrentContext().preCompletion(() -> {
						throw toThrow;
					});
				return connection.createStatement()
					.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
			});
			fail("An exception should occur!");
			// We have to catch Exception as the compiler complains
			// otherwise
		} catch (TransactionRolledBackException tre) {
			assertSame(toThrow, tre.getCause());
		}
		
		assertRollback();
	}

	@Test
	public void testNoRollbackForException() {
		RuntimeException toThrow = new RuntimeException("Bang!");
		
		try {
			txControl.build()
				.noRollbackFor(RuntimeException.class)
				.required(() -> {
						PreparedStatement ps = connection
								.prepareStatement("Insert into TEST_TABLE values ( ? )");
						
						ps.setString(1, "Hello World!");
						ps.executeUpdate();
						
						throw toThrow;
					});
			fail("An exception should occur!");
			// We have to catch Exception as the compiler complains
			// otherwise
		} catch (ScopedWorkException swe) {
			assertSame(toThrow, swe.getCause());
		}
		
		assertEquals("1: Hello World!", txControl.notSupported(() -> {
			Statement s = connection.createStatement();
			
			ResultSet rs = s.executeQuery("Select count(*) from TEST_TABLE");
			rs.next();
			int count = rs.getInt(1);
			
			rs = s.executeQuery("Select message from TEST_TABLE ORDER BY message");
			
			rs.next();
			return "" + count + ": " + rs.getString(1);
		}));
	}

	private void assertRollback() {
		assertEquals(Integer.valueOf(0), txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement()
					.executeQuery("Select count(*) from TEST_TABLE");
			rs.next();
			return rs.getInt(1);
		}));
	}
}
