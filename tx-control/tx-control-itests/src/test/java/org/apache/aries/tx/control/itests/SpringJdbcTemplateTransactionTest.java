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
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SpringJdbcTemplateTransactionTest extends AbstractTransactionTest {

	JdbcTemplate jdbcTemplate; 

	@Before
	public void setUp() {
		super.setUp();
		
		jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, false));
	}
	
	@Test
	public void testJdbcTemplateTx() {
		
		StatementCallback<Boolean> callback = s -> 
			s.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
		
		txControl.required(() -> jdbcTemplate.execute(callback));
		
		assertEquals("Hello World!", txControl.notSupported(() -> 
			jdbcTemplate.queryForObject("Select * from TEST_TABLE", String.class)));
	}
	
	@Test
	public void testJdbcTemplateRollback() {
		
		StatementCallback<Boolean> callback = s -> 
			s.execute("Insert into TEST_TABLE values ( 'Hello World!' )");
		
		txControl.required(() -> {
			jdbcTemplate.execute(callback);
			txControl.setRollbackOnly();
			return null;
		});

		assertEquals(Integer.valueOf(0), txControl.notSupported(() -> 
			jdbcTemplate.queryForInt("Select count(*) from TEST_TABLE")));
	}
	
	
	@Override
	protected Option testSpecificOptions() {
		return composite(
				mavenBundle("org.springframework", "spring-jdbc").versionAsInProject(),
				mavenBundle("org.springframework", "spring-tx").versionAsInProject(),
				mavenBundle("org.springframework", "spring-beans").versionAsInProject(),
				mavenBundle("org.springframework", "spring-core").versionAsInProject(),
				mavenBundle("org.springframework", "spring-context").versionAsInProject());
	}

}
