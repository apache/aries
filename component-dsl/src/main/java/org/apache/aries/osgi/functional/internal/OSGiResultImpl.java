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

/**
 * @author Carlos Sierra Andrés
 */
public class OSGiResultImpl<T> implements OSGiResult<T> {

	public Pipe<?, Tuple<T>> added;
	public Pipe<?, Tuple<T>> removed;
	public Runnable start;
	public Runnable close;

	public OSGiResultImpl(
		Pipe<?, Tuple<T>> added, Pipe<?, Tuple<T>> removed,
		Runnable start, Runnable close) {

		this.added = added;
		this.removed = removed;
		this.start = start;
		this.close = close;
	}

	@Override
	public void close() {
		close.run();
	}

}
