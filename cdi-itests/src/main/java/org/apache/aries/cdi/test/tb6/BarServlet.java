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

package org.apache.aries.cdi.test.tb6;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.cdi.annotations.ComponentPropertyType;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;

@SingleComponent
@Service(Servlet.class)
@BarServlet.Props
public class BarServlet extends HttpServlet {

	@Retention(RUNTIME) @Target(TYPE )
	@ComponentPropertyType
	public @interface Props {
		String osgi_http_whiteboard_servlet_name() default "bar";
		String osgi_http_whiteboard_servlet_pattern() default "/bar";

	}

	private static final long serialVersionUID = 1L;

	@Override
	protected void service(
			HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		String name = request.getParameter("name");

		if (name != null) {
			requestData.setData(name);
		}

		response.setContentType("text/plain");

		try (PrintWriter writer = response.getWriter()) {
			if (requestData.hasData()) {
				writer.print(requestData.getData());
			}
		}
	}

	@Inject
	RequestData requestData;

}
