# How to run the tck tests 

# First you need to retrieve the tck and deploy the test jpa test jar (these steps only need to be done once)
# 1. Request access to the OSGi tck. http://felix.apache.org/documentation/development/using-the-osgi-compliance-tests.html
# 2. Get and extract https://svn.apache.org/repos/tck/osgi-cts/osgi.enterprise.tests/5.0.0/osgi.ct.enterprise.jar
# 3. deploy the test jar to maven
    mvn install:install-file -Dfile=jar/org.osgi.test.cases.jpa-5.0.0.jar -DgroupId=org.apache.aries.tck -DartifactId=org.osgi.test.cases.jpa -Dversion=5.0.0 -Dpackaging=jar

# 3. execute the commands below
mvn clean install
./runtests

