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
package org.apache.aries.pushstream;

import static org.apache.aries.pushstream.AbstractPushStreamImpl.State.*;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;

public class IntermediatePushStreamImpl<T> extends AbstractPushStreamImpl<T>
		implements PushStream<T> {
	
	private final AbstractPushStreamImpl< ? > previous;
	
	protected IntermediatePushStreamImpl(PushStreamProvider psp,
			Executor executor, ScheduledExecutorService scheduler,
			AbstractPushStreamImpl< ? > previous) {
		super(psp, executor, scheduler);
		this.previous = previous;
	}

	@Override
	protected boolean begin() {
		if(closed.compareAndSet(BUILDING, STARTED)) {
			beginning();
			previous.begin();
			return true;
		}
		return false;
	}

	protected void beginning() {
		// The base implementation has nothing to do, but
		// this method is used in windowing
	}
	
}
