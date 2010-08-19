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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.quiesce.manager.itest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.osgi.framework.Bundle;

public class MockQuiesceParticipant implements QuiesceParticipant {

	public static final int RETURNIMMEDIATELY = 0;
	public static final int NEVERRETURN = 1;
	public static final int WAIT = 2;
	private int behaviour;
	private List<QuiesceCallback> callbacks = new ArrayList<QuiesceCallback>();
	private ExecutorService executor = Executors.newCachedThreadPool();
	private int started = 0;
	private int finished = 0;
	
	public MockQuiesceParticipant( int i ) {
		behaviour = i;
	}

	public void quiesce(final QuiesceCallback callback, final List<Bundle> bundlesToQuiesce) {
		Runnable command = new Runnable() {
			public void run() {
				started += 1;
				callbacks.add(callback);
				switch (behaviour) {
				case 0:
					//return immediately
					System.out.println("MockParticipant: return immediately");
					callback.bundleQuiesced(bundlesToQuiesce.toArray(new Bundle[bundlesToQuiesce.size()]));
					callbacks.remove(callback);
					finished += 1;
					break;
				case 1:
					//just don't do anything
					System.out.println("MockParticipant: just don't do anything");
					break;
				case 2:
					//Wait for 5s then quiesce
					System.out.println("MockParticipant: Wait for 5s then quiesce");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
					}
					callback.bundleQuiesced(bundlesToQuiesce.toArray(new Bundle[bundlesToQuiesce.size()]));
					callbacks.remove(callback);
					finished += 1;
					break;
				default: 
					//Unknown behaviour, don't do anything
				}
			}
		};
		executor.execute(command);
	}

	public int getStartedCount() {
		return started;
	}
	
	public int getFinishedCount() {
		return finished;
	}
	
	public synchronized void reset() {
		started = 0;
		finished = 0;
	}
	
	
}
