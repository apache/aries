/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

public class SubsystemIdentifier {
	private static long lastId;
	
	synchronized static long getLastId() {
		return lastId;
	}
	
	synchronized static long getNextId() {
		if (Long.MAX_VALUE == lastId)
			throw new IllegalStateException("The next subsystem ID would exceed Long.MAX_VALUE: " + lastId);
		// First ID will be 1.
		return ++lastId;
	}
	
	synchronized static void setLastId(long id) {
		lastId = id;
	}
}
