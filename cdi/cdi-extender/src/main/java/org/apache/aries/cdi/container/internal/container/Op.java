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

package org.apache.aries.cdi.container.internal.container;

import java.util.Arrays;

public class Op {

	public static enum Mode {CLOSE, OPEN}

	public static enum Type {
		CONFIGURATION_EVENT,
		CONFIGURATION_LISTENER,
		CONTAINER_ACTIVATOR,
		CONTAINER_BOOTSTRAP,
		CONTAINER_COMPONENT,
		CONTAINER_FIRE_EVENTS,
		CONTAINER_INSTANCE,
		CONTAINER_PUBLISH_SERVICES,
		REFERENCES,
		EXTENSION,
		FACTORY_ACTIVATOR,
		FACTORY_COMPONENT,
		FACTORY_INSTANCE,
		INIT,
		SINGLE_ACTIVATOR,
		SINGLE_COMPONENT,
		SINGLE_INSTANCE,
	}

	public static Op of(Mode mode, Type type, String name) {
		return new Op(mode, type, name);
	}

	private Op(Mode mode, Type type, String name) {
		this.mode = mode;
		this.type = type;
		this.name = name;
	}

	public final Mode mode;
	public final Type type;
	public final String name;

	@Override
	public String toString() {
		return Arrays.asList(getClass().getSimpleName(), mode, type, name).toString();
	}

}