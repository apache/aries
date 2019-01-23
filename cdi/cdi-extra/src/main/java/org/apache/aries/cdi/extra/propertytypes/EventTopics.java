/*
 * Copyright (c) OSGi Alliance (2018, 2019). All Rights Reserved.
 *
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

package org.apache.aries.cdi.extra.propertytypes;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.service.cdi.annotations.BeanPropertyType;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.annotations.RequireEventAdmin;

/**
 * Component Property Type for the {@link EventConstants#EVENT_TOPIC} service
 * property of an {@link EventHandler} service.
 * <p>
 * This annotation can be used on an {@link EventHandler} component to declare
 * the values of the {@link EventConstants#EVENT_TOPIC} service property.
 */
@BeanPropertyType
@RequireEventAdmin
@Retention(RUNTIME)
@Target({FIELD, METHOD, TYPE})
public @interface EventTopics {

	public static final class Literal extends AnnotationLiteral<EventTopics> implements EventTopics {

		private static final long serialVersionUID = 1L;

		public static final Literal of(String[] value) {
			return new Literal(value);
		}

		private Literal(String[] value) {
			_value = value;
		}

		@Override
		public String[] value() {
			return _value;
		}

		private final String[] _value;
	}

	/**
	 * Service property specifying the {@code Event} topics of interest to an
	 * {@link EventHandler} service.
	 *
	 * @return The event topics.
	 * @see EventConstants#EVENT_TOPIC
	 */
	String[] value();
}
