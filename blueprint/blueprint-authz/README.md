<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements. See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version
    2.0 (the "License"); you may not use this file except in compliance
    with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0 Unless required by
    applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
    CONDITIONS OF ANY KIND, either express or implied. See the License for
    the specific language governing permissions and limitations under the
    License.
-->
Blueprint extension for role based access control based on JAAS and JEE annotations
===================================================================================

An aries blueprint extension that supports role based access control based on a JAAS login and the JEE @RolesAllowed annotation.

install -s mvn:org.apache.aries.blueprint/org.apache.aries.blueprint.authz/1.0.0-SNAPSHOT

To use it add the authz namespace xmlns:authz="http://aries.apache.org/xmlns/authorization/v1.0.0" to your blueprint file and place a <authz:enable/> element at the start of your context.

This will enable annotation scanning for all beans in the context. For bean classes that have the @RolesAllowed annotation an Authorization interceptor will be added. This interceptor will read the JAAS Subject from AccesControlContext and use the principles there to do the authorization.

Sample blueprint snippet

<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0" xmlns:authz="http://aries.apache.org/xmlns/authorization/v1.0.0">
    <authz:enable/>
    <bean id="personServiceImpl" class="net.lr.tutorial.karaf.cxf.personservice.impl.PersonServiceImpl"/>
</blueprint>

