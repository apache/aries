/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.jndiurl.itest;

import org.apache.aries.jndiurl.itest.beans.ConfigBean;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class JndiUrlItestServlet extends HttpServlet {

    private static final long serialVersionUID = -4610850218411296469L;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder result = new StringBuilder();
        try {
            InitialContext ctx = new InitialContext();
            ConfigBean cb = (ConfigBean) ctx.lookup("blueprint:comp/config");
            result.append(cb.getSimple().getOwner());
            result.append(".");
            result.append(cb.getVersion());  // Expected output is now "Mark.2.0"

            // Now lookup and use a service published from another bundle
            @SuppressWarnings("unchecked")
            List<String> listService = (List<String>) ctx.lookup("blueprint:comp/listRef");
            result.append(".");
            String thirdListEntry = listService.get(2);
            result.append(thirdListEntry);
        } catch (NamingException nx) {
            IOException ex = new IOException(nx.getMessage());
            ex.initCause(nx);
            throw ex;
        }
        resp.getWriter().print(result.toString());
        resp.getWriter().close();
    }
}
