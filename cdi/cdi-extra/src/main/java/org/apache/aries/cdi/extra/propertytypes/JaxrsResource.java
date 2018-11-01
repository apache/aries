/*
 * Copyright (c) OSGi Alliance (2017, 2018). All Rights Reserved.
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

import org.osgi.service.cdi.annotations.BeanPropertyType;
import org.osgi.service.jaxrs.whiteboard.annotations.RequireJaxrsWhiteboard;

/**
 * Component Property Type for the {@code osgi.jaxrs.resource} service property.
 * <p>
 * This annotation can be used on a JAX-RS resource to declare the value of the
 * {@link org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants#JAX_RS_RESOURCE}
 * service property.
 */
@BeanPropertyType
@RequireJaxrsWhiteboard
@Retention(RUNTIME)
@Target({FIELD, METHOD, TYPE})
public @interface JaxrsResource {
	/**
	 * Prefix for the property name. This value is prepended to each property
	 * name.
	 */
	String PREFIX_ = "osgi.";

	// This is a marker annotation.
}
