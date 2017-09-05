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

import org.osgi.service.cdi.annotations.ServiceEvent;

public class ServiceEventImpl<T> implements ServiceEvent<T> {

	public static enum Event { ADDING, MODIFIED, REMOVED }

	private final T _t;
	private final ServiceEventImpl.Event _event;

	public ServiceEventImpl(T t, ServiceEventImpl.Event event) {
		_t = t;
		_event = event;
	}

	@Override
	public ServiceEvent<T> adding(Consumer<T> consumer) {
		if (_event == Event.ADDING) {
			consumer.accept(_t);
		};
		return this;
	}

	@Override
	public ServiceEvent<T> modified(Consumer<T> consumer) {
		if (_event == Event.MODIFIED) {
			consumer.accept(_t);
		}
		return this;
	}

	@Override
	public ServiceEvent<T> removed(Consumer<T> consumer) {
		if (_event == Event.REMOVED) {
			consumer.accept(_t);
		}
		return this;
	}

}