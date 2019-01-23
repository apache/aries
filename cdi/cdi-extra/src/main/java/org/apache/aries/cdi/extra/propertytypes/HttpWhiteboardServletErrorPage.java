/*
 * Copyright (c) OSGi Alliance (2017, 2019). All Rights Reserved.
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
import org.osgi.service.http.whiteboard.annotations.RequireHttpWhiteboard;

/**
 * Component Property Type for the
 * {@code osgi.http.whiteboard.servlet.errorPage} service property.
 * <p>
 * This annotation can be used on a {@link javax.servlet.Servlet} to declare the
 * value of the
 * {@link org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_SERVLET_ERROR_PAGE
 * HTTP_WHITEBOARD_SERVLET_ERROR_PAGE} service property.
 */
@BeanPropertyType
@RequireHttpWhiteboard
@Retention(RUNTIME)
@Target({FIELD, METHOD, TYPE})
public @interface HttpWhiteboardServletErrorPage {

	public static final class Literal extends AnnotationLiteral<HttpWhiteboardServletErrorPage> implements HttpWhiteboardServletErrorPage {

		private static final long serialVersionUID = 1L;

		public static final Literal of(String[] errorPage) {
			return new Literal(errorPage);
		}

		private Literal(String[] errorPage) {
			_errorPage = errorPage;
		}

		@Override
		public String[] errorPage() {
			return _errorPage;
		}

		private final String[] _errorPage;
	}

	/**
	 * Prefix for the property name. This value is prepended to each property
	 * name.
	 */
	String PREFIX_ = "osgi.http.whiteboard.servlet.";

	/**
	 * Service property identifying the error pages of a servlet.
	 *
	 * @return The servlet error pages.
	 * @see org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_SERVLET_ERROR_PAGE
	 *      HTTP_WHITEBOARD_SERVLET_ERROR_PAGE
	 */
	String[] errorPage();
}
