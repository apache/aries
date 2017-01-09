/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.sample;

import org.apache.aries.blueprint.web.BlueprintContextListener;
import org.osgi.service.blueprint.container.BlueprintContainer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class AccountsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BlueprintContainer container = (BlueprintContainer) getServletContext().getAttribute(BlueprintContextListener.CONTAINER_ATTRIBUTE);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");

        List<Account> accounts = (List<Account>) container.getComponentInstance("accounts");
        sb.append("<h2>Accounts</h2>");
        sb.append("<ul>");
        for (Account account : accounts) {
            sb.append("<li>").append(account.getAccountNumber()).append("</li>");
        }
        sb.append("</ul>");

        sb.append("<br/>");

        Foo foo = (Foo) container.getComponentInstance("foo");
        sb.append("<h2>Foo</h2>");
        sb.append("<ul>");
        sb.append("<li>").append("a = ").append(foo.getA()).append("</li>");
        sb.append("<li>").append("b = ").append(foo.getB()).append("</li>");
        sb.append("<li>").append("currency = ").append(foo.getCurrency()).append("</li>");
        sb.append("<li>").append("date = ").append(foo.getDate()).append("</li>");
        sb.append("</ul>");

        sb.append("</body></html>");

        resp.getWriter().write(sb.toString());
    }
}
