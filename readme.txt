BUILDING
========

Follow the procedure for building Aries described at http://aries.apache.org/development/buildingaries.html.


RUNNING
=======

The following bundles are required to run subsystems. All are available in at least one of the repositories configured within the Aries parent POM.

mavenBundle("org.apache.aries",             "org.apache.aries.util").version("1.0.1-SNAPSHOT"),
mavenBundle("org.apache.aries.application", "org.apache.aries.application.api").version("1.0.0"),
mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller").version("1.0.0"),
mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils").version("1.0.0"),
mavenBundle("org.apache.aries.blueprint",   "org.apache.aries.blueprint").version("1.0.0"),
mavenBundle("org.apache.aries.proxy",       "org.apache.aries.proxy").version("1.0.1-SNAPSHOT"),
mavenBundle("org.apache.aries.subsystem",   "org.apache.aries.subsystem").version("1.0.0-SNAPSHOT"),
mavenBundle("org.apache.felix",             "org.apache.felix.resolver")version("0.1.0-SNAPSHOT"),,
mavenBundle("org.eclipse.equinox",          "org.eclipse.equinox.coordinator").version("1.1.0.v20120522-1841"),
mavenBundle("org.eclipse.equinox",          "org.eclipse.equinox.region").version("1.1.0.v20120522-1841")

Note that the various org.apache.aries.application.* bundles may be replaced with the "org.apache.aries.application", version "1.0.0", uber bundle if desired.

A framework implementing version R5 of the OSGi specification is also needed. For example:

mavenBundle("org.eclipse", "org.eclipse.osgi").version("3.8.0.v20120529-1548")


USING
=====

When installing subsystems from a directory structure, note that the names of nested directories representing child subsystems must end with ".esa", and those representing bundles must end with ".jar".


KNOWN ISSUES
============

(1)
Does not currently support service dependencies provided by child subsystems to the parent. This will fail with a resolution exception because, during installation of the parent along with its child, the child has not reached the state where service capabilities have been computed before they are needed.

