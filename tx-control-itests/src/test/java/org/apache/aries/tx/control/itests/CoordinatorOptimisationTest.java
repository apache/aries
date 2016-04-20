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

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CoordinatorOptimisationTest extends AbstractTransactionTest implements Participant {

	protected Option testSpecificOptions() {
		return mavenBundle("org.apache.felix", "org.apache.felix.coordinator").versionAsInProject();
	}
	
    @Inject
    Coordinator coordinator;
    
    @Test
    public void compareWithAndWithoutCoord() {
    	String base = "Hello ";
    	
    	AtomicInteger counter = new AtomicInteger(1);
    	
    	List<String> messages = generate(() -> base + counter.getAndIncrement())
    				.limit(10000)
    				.collect(toList());
    	
    	long noCoord;
    	long withCoord;
    	
    	long start = System.currentTimeMillis();
    	try {
	    	messages.stream()
	    		.forEach(this::persistMessage);
    	} finally {
    		noCoord = System.currentTimeMillis() - start;
    	}
    	
    	txControl.required(() -> connection.createStatement().executeUpdate("DELETE FROM TEST_TABLE"));
    	
    	coordinator.begin("foo", MINUTES.toMillis(5));
    	start = System.currentTimeMillis();
    	try {
    		messages.stream()
    			.forEach(this::persistMessage);
    	} finally {
    		coordinator.peek().end();
    		withCoord = System.currentTimeMillis() - start;
    	}
    	
    	System.out.println("\n\n\n\nWithout Coord: " + noCoord + "  With Coord: " + withCoord);
    }
    
    @SuppressWarnings("unchecked")
	private void persistMessage(String message) {
        if(coordinator.addParticipant(this)) {
            ((List<String>)coordinator.peek().getVariables()
                .computeIfAbsent(getClass(), k -> new ArrayList<String>()))
                .add(message);
        } else {
            txControl.required(() -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "Insert into TEST_TABLE values ( ? )");
                    ps.setString(1, message);
                    return ps.executeUpdate();
                });
        }
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public void ended(Coordination coord) throws Exception {
        txControl.required(() -> {
                List<String> l = (List<String>) coord.getVariables()
                                .get(getClass());
                
                PreparedStatement ps = connection.prepareStatement(
                        "Insert into TEST_TABLE values ( ? )");
                
                l.stream().forEach(s -> {
                	 	try {
	                        ps.setString(1, s);
	                        ps.addBatch();
                	 	} catch (SQLException sqle) {
                	 		throw new RuntimeException(sqle);
                	 	}
                    });
                
                return ps.executeBatch();
            });
    }

    @Override
    public void failed(Coordination arg0) throws Exception { }
}
