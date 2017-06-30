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


import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ConnectionRefreshTest extends AbstractTransactionTest {

	private static final String COUNT_CONNECTION_CHECKS = "Select EXECUTION_COUNT from INFORMATION_SCHEMA.QUERY_STATISTICS WHERE SQL_STATEMENT='Select COUNT(*) from TEST_TABLE'";

	private static final String COUNT_SESSIONS = "Select COUNT(*) from INFORMATION_SCHEMA.SESSIONS";

	private static final int CONNECTIONS = 5;	
	
	@Override
	protected Map<String, Object> resourceProviderConfig() {
		Map<String, Object> config = new HashMap<>();
		
		// Add a test query for the aries.connection.test.query property so that the test query will be
		// used by the Hikari pooling library to test if a connection is okay as opposed to using the default 
		// Connection.isValid() method.
		config.put("aries.connection.test.query", "Select COUNT(*) from TEST_TABLE");
		
		config.put(JDBCConnectionProviderFactory.MAX_CONNECTIONS, CONNECTIONS);
		config.put(JDBCConnectionProviderFactory.MIN_CONNECTIONS, CONNECTIONS);		
		return config;
	}	
	
	/**
	 * Test that setting the aries.connection.test.query property will indeed be used by the Hikari pooling 
	 * library to test if a connection is okay as opposed to using the default Connection.isValid() method.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testConnectionTestQuery() throws Exception {

		System.out.println("-------------- start testConnectionTestQuery -------------------------");			

		// Enable statistics gathering so that all connection checks will be logged
		
		txControl.required(() -> 
			connection.createStatement().executeUpdate("SET QUERY_STATISTICS TRUE"));
		
		// Wait a second so that the pool will re-check connection liveness the next time 
		// that we use one
		
		Thread.sleep(1000);

		// Spin up some threads to execute some queries to ensure all connections in the pool
		// are checked for liveness
		List<Thread> threads = Stream.generate(() -> new Thread(new ExecuteQuery()))
					.limit(CONNECTIONS * 2)
					.collect(toList());
		
		threads.stream().forEach(Thread::start);
		
		System.out.println("testConnectionTestQuery() - Waiting while queries are run");
		threads.stream().forEach(t -> { 
				try {
					t.join(5000);
				} catch (InterruptedException ie) {}
				assertFalse("The query did not complete in time", t.isAlive());
			});
		
		// Run a query to check that there are the expected number of connections to the Db server 		
		txControl.notSupported(() -> {

			// First check we saturated the pool
			System.out.println("testConnectionTestQuery() - Execute query to get number of active connections");				
			ResultSet rs = connection.createStatement().executeQuery(COUNT_SESSIONS);

			assertTrue(rs.next()); 

			int numberOfConnections = rs.getInt(1);
			assertNotNull(numberOfConnections);				
			assertEquals("Number of connections " + numberOfConnections, CONNECTIONS, numberOfConnections);
				
			// Now check that each connection was tested for liveness
			rs = connection.createStatement().executeQuery(COUNT_CONNECTION_CHECKS);
			
			assertTrue(rs.next()); 

			assertEquals("There should be a check for every connection", CONNECTIONS, rs.getInt(1));
			
			return null;

		});			
		

		System.out.println("-------------- end testConnectionTestQuery -------------------------");			

	}	

	public class ExecuteQuery implements Runnable {
		
		public void run() {		
			
			txControl.required(() -> {
				
				System.out.println("   ExecuteQuery - query to get number of active connections after Db server has restarted");				
				ResultSet rs = connection.createStatement().executeQuery(COUNT_SESSIONS);
	
				if(rs.next()) {
	
					Integer numberOfConnections = rs.getInt(1);
					System.out.println("   ExecuteQuery - numberOfConnections after starting Db server =<" + numberOfConnections + ">");
	
				}
				// A short sleep to ensure contention for connections
				Thread.sleep(100);
	
				return null;
	
			});					
		}		
	}
}