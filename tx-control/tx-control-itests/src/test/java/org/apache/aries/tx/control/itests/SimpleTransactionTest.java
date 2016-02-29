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

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SimpleTransactionTest extends AbstractTransactionTest {

	@Test
	public void testTx() {
		txControl.required(() -> connection.createStatement()
				.execute("Insert into TEST_TABLE values ( 'Hello World!' )"));

		assertEquals("Hello World!", txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement()
					.executeQuery("Select * from TEST_TABLE");
			rs.next();
			return rs.getString(1);
		}));
	}

	@Test
	public void testRollback() {
		txControl.required(() -> {
			connection.createStatement().execute(
					"Insert into TEST_TABLE values ( 'Hello World!' )");
			txControl.setRollbackOnly();
			return null;
		});

		assertEquals(Integer.valueOf(0), txControl.notSupported(() -> {
			ResultSet rs = connection.createStatement()
					.executeQuery("Select count(*) from TEST_TABLE");
			rs.next();
			return rs.getInt(1);
		}));
	}

	@Test
	public void testNestedTx() {
		txControl.required(() -> {
			connection.createStatement()
				.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
			
			txControl.requiresNew(() -> connection.createStatement()
					.execute("Insert into TEST_TABLE values ( 'Hello Nested World!' )"));
			
			return null;
		});
		
		String[] results = txControl.notSupported(() -> {
				Statement s = connection.createStatement();
				
				ResultSet rs = s.executeQuery("Select count(*) from TEST_TABLE");
				rs.next();
				int count = rs.getInt(1);
				
				rs = s.executeQuery("Select message from TEST_TABLE ORDER BY message");
				
				String[] result = new String[2];
				rs.next();
				result[0] = "" + count + ": " + rs.getString(1);
				rs.next();
				result[1] = "" + count + ": " + rs.getString(1);
				return result;
			});
		
		System.out.println(Arrays.toString(results));
		
		assertEquals("2: Hello Nested World!", results[0]);
		assertEquals("2: Hello World!", results[1]);
	}
	
	@Test
	public void testNestedTxOuterRollback() {
		txControl.required(() -> {
				// This will not end up in the database
				connection.createStatement()
					.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
				
				// This should only apply to the current transaction level
				txControl.setRollbackOnly();
			
				// This nested transaction will commit
				txControl.requiresNew(() -> connection.createStatement()
						.execute("Insert into TEST_TABLE values ( 'Hello Nested World!' )"));
			
				return null;
			});
		
		assertEquals("1: Hello Nested World!", txControl.notSupported(() -> {
				Statement s = connection.createStatement();
				
				ResultSet rs = s.executeQuery("Select count(*) from TEST_TABLE");
				rs.next();
				int count = rs.getInt(1);
				
				rs = s.executeQuery("Select message from TEST_TABLE ORDER BY message");
				
				rs.next();
				return "" + count + ": " + rs.getString(1);
			}));
	}

	@Test
	public void testNestedTxInnerRollback() {
		txControl.required(() -> {
			// This will end up in the database
			connection.createStatement()
				.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
			
						
			// This nested transaction not commit
			txControl.requiresNew(() -> {
					connection.createStatement()
						.execute("Insert into TEST_TABLE values ( 'Hello Nested World!' )");
					txControl.setRollbackOnly();
					return null;
				});
			
			return null;
		});
		
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
	
	@Test
	public void testRequiredInheritsTx() {
		txControl.required(() -> {
			Object key = txControl.getCurrentContext().getTransactionKey();
			
			connection.createStatement()
				.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
			
			return txControl.required(() -> {
					assertEquals(key , txControl.getCurrentContext().getTransactionKey());
					return connection.createStatement()
							.execute("Insert into TEST_TABLE values ( 'Hello Nested World!' )");
				});
		});
		
		String[] results = txControl.notSupported(() -> {
				Statement s = connection.createStatement();
				
				ResultSet rs = s.executeQuery("Select count(*) from TEST_TABLE");
				rs.next();
				int count = rs.getInt(1);
				
				rs = s.executeQuery("Select message from TEST_TABLE ORDER BY message");
				
				String[] result = new String[2];
				rs.next();
				result[0] = "" + count + ": " + rs.getString(1);
				rs.next();
				result[1] = "" + count + ": " + rs.getString(1);
				return result;
			});
		
		System.out.println(Arrays.toString(results));
		
		assertEquals("2: Hello Nested World!", results[0]);
		assertEquals("2: Hello World!", results[1]);
	}
}
