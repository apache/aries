/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.osgi.functional.internal;

import org.apache.aries.osgi.functional.OSGiResult;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Carlos Sierra Andrés
 */
public class OSGiResultImpl implements OSGiResult {

	public OSGiResultImpl(Runnable start, Runnable close) {
		this.start = start;
		this.close = close;
	}

	@Override
	public void close() {
		while (!_working.compareAndSet(false, true)) {
			Thread.yield();
		}

		if (_closed.compareAndSet(false, true) && _started) {
			close.run();
		}

		_working.set(false);
	}

	@Override
	public void start() {
		if (_working.compareAndSet(false, true)) {

			if (!_started && !_closed.get()) {
				start.run();

				_started = true;
			}

			_working.set(false);
		}

	}

	private final Runnable start;
	private final Runnable close;
	private AtomicBoolean _working = new AtomicBoolean();
	private AtomicBoolean _closed = new AtomicBoolean();
	private volatile boolean _started = false;

}
