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

package org.apache.aries.cdi.container.test.beans;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.reference.ReferenceEvent;
import org.osgi.service.cdi.reference.ReferenceServiceObjects;

public class ReferenceEventImpl<T> implements ReferenceEvent<T> {

	enum Event { ADDING, MODIFIED, REMOVED }
	private final T _t;
	private final ReferenceEventImpl.Event _event;

	public ReferenceEventImpl(T t, ReferenceEventImpl.Event event) {
		_t = t;
		_event = event;
	}

	@Override
	public void onAdding(Consumer<T> action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddingServiceReference(Consumer<ServiceReference<T>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddingServiceObjects(Consumer<ReferenceServiceObjects<T>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddingProperties(Consumer<Map<String, ?>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddingTuple(Consumer<Entry<Map<String, ?>, T>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUpdate(Consumer<T> action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUpdateServiceReference(Consumer<ServiceReference<T>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUpdateServiceObjects(Consumer<ReferenceServiceObjects<T>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUpdateProperties(Consumer<Map<String, ?>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUpdateTuple(Consumer<Entry<Map<String, ?>, T>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRemove(Consumer<T> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRemoveServiceReference(Consumer<ServiceReference<T>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRemoveServiceObjects(Consumer<ReferenceServiceObjects<T>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRemoveProperties(Consumer<Map<String, ?>> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRemoveTuple(Consumer<Entry<Map<String, ?>, T>> consumer) {
		// TODO Auto-generated method stub
		
	}

/*	@Override
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
*/
}