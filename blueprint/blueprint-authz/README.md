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

