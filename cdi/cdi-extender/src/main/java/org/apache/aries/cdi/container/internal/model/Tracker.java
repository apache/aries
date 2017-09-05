/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.reference.ReferenceCallback;
import org.osgi.util.tracker.ServiceTracker;

public abstract class Tracker {

	public abstract <T> void track(String targetFilter, ReferenceCallback callback);

	public int size() {
		return trackers.size();
	}

	public void close() {
		trackers.removeIf(
			t -> {
				t.close();

				return true;
			}
		);
	}

	public void open() {
		trackers.stream().forEach(t -> t.open());
	}

	protected final List<ServiceTracker<Object, ?>> trackers = new CopyOnWriteArrayList<>();

}
