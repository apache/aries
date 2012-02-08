The Subsystems subproject uses some unreleased and difficult to find equinox 
artifacts.

There's a profile to download them automatically like this:

mvn clean install -Pfetch-external

The normal build is now in the default profile so if you are running any other 
profiles you will need

mvn clean install -Pdefault,<your other profiles>

PLEASE if you change the version of these jars used in subsystems UPDATE THE 
DOWNLOAD INFO in the subsystem root pom.

The downloader is from here:

http://code.google.com/p/maven-external-dependency-plugin/

ADDENDUM

In order to use the downloader, you must do

svn co http://maven-external-dependency-plugin.googlecode.com/svn/trunk/maven-external-dependency-plugin/ maven-external-dependency-plugin

then run "mvn install" from the maven-external-dependency-plugin directory so 
that it is accessible from your local m2 repository before running 

mvn clean install -Pfetch-external 

from the subsystem project. If successful, you must the also run 

mvn clean install -Pdefault 

from the aries/subsystem directory. Additionally, you will need dependencies in 
your local m2 repository obtained from executing "mvn clean install" from the 
aries/util/util and aries/application directories.