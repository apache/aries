/**
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.blueprint.context;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Apr 14, 2009
 * Time: 12:17:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class WaitForDependencyException extends Exception {

    private final String[] serviceObjectClass;
    private final String serviceFilter;

    public WaitForDependencyException(String[] serviceObjectClass, String serviceFilter) {
        this.serviceObjectClass = serviceObjectClass;
        this.serviceFilter = serviceFilter;
    }

    public WaitForDependencyException(String serviceObjectClass, String serviceFilter) {
        this(new String[] { serviceObjectClass }, serviceFilter);
    }

    public String[] getServiceObjectClass() {
        return serviceObjectClass;
    }

    public String getServiceFilter() {
        return serviceFilter;
    }
}
