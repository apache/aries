# Aries JPA

Implements the OSGi JPA Service Specification from the enterprise spec. Additionally some convenience
services are provided to make it easier to use JPA in blueprint and DS.

http://aries.apache.org/modules/jpaproject.html

# Running tck tests

See itests/jpa-tck-itest/README.txt

# Releasing

Run the tck tests to make sure we are still conforming to the spec.

mvn clean deploy
mvn release:prepare -Darguments="-DskipTests"
mvn release:perform

After the release make sure to adapt the versions in the tck test modules.
