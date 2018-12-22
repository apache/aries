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

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletName;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletPattern;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;

@HttpWhiteboardServletName("bar")
@HttpWhiteboardServletPattern("/bar")
@Requirement(
	namespace = CDIConstants.CDI_EXTENSION_PROPERTY,
	name = "aries.cdi.http"
)
@Service(Servlet.class)
@SingleComponent
@SuppressWarnings("serial")
public class BarServlet extends HttpServlet {

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
