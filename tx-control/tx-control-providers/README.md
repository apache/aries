Apache Aries OSGi Transaction Control Resource Providers
--------------------------------------------------------

This project contains modules which implement OSGi Transaction Control Resource Providers.

The Transaction Control Service is defined in Chapter 147 of the OSGi R7 specification. This specification is not yet final, but public drafts of this specification are available from the OSGi Alliance.

Given that the RFC is non-final the OSGi API declared in this project is subject to change at any time up to its official release. Also the behaviour of this implementation may not always be up-to-date with the latest wording in the RFC. The project maintainers will, however try to keep pace with the RFC, and to ensure that the implementations are compliant with any OSGi specifications that result from the RFC.

# Modules

The following Resource Provider types are available:

1. jdbc :- Resource Providers that work with JDBC resources and the OSGi JDBC service
2. jpa :- Resource Providers that work with JPA resources and the OSGi JPA service
