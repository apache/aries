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
package org.apache.aries.util.filesystem;

import java.io.Closeable;

/**
 * Implementation of IDirectory that is more efficient for batch operations as it does not due 
 * automatic resource management. Instead the user has to explicitly call close to release resources.
 * Resources are cached for the current IDirectory archive only. Nested archives should be converted to 
 * {@link ICloseableDirectory} separately.
 */
public interface ICloseableDirectory extends IDirectory, Closeable {
	/**
	 * Checks whether the closeable directory has been closed
	 */
	boolean isClosed();
}
