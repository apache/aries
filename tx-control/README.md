Apache Aries OSGi Transaction Control Service implementations
-------------------------------------------------------------

This project contains sets of modules which implement parts of the OSGi Transaction Control Service specification. This includes implementations of the Transaction Control Service and various Resource Provider services implementations, such as JDBC and JPAn.

The Transaction Control Service is defined in Chapter 147 of the OSGi R7 specification. This specification is not yet final, but public drafts of this specification are available from the OSGi Alliance.

Given that the RFC is non-final the OSGi API declared in this project is subject to change at any time up to its official release. Also the behaviour of this implementation may not always be up-to-date with the latest wording in the RFC. The project maintainers will, however try to keep pace with the RFC, and to ensure that the implementations are compliant with any OSGi specifications that result from the RFC.

# Modules

The following modules are available for use in OSGi

1. tx-control-services :- This project contains implementations of the OSGi Transaction Control Service. You will need to select one appropriate for your needs. 
2. tx-control-providers :- This project contains implementations of OSGi Transaction Control Resource Providers. You will need an appropriate resource provider for each type of resource that you want to interact with.
3. tx-control-parent := The parent pom for modules produced by this project.


## Which modules should I use?

If you wish to use entirely lightweight, resource-local transactions then it is best to pair a local service implementation with the relevant local provider.

If two-phase commit is needed across multiple resources then an XA capable service implementation and relevant xa capable resource provider(s) must be used.

It is not advised to use multiple Transaction Control Service implementations at the same time. This will be confusing, and may lead to problems if different parts of your application bind to different service implementations. If you do choose to deploy multiple Transaction Control services then please ensure that the different parts of your application target the correct service implementation.
