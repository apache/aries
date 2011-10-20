The Subsystems subproject uses some unreleased and difficult to find equinox artifacts.

There's a profile to download them automatically like this:

mvn clean install -Pfetch-external

The normal build is now in the default profile so if you are running any other profiles you will need

mvn clean install -Pdefault,<your other profiles>

PLEASE if you change the version of these jars used in subsystems UPDATE THE DOWNLOAD INFO in the subsystem root pom.

The downloader is from here:

http://code.google.com/p/maven-external-dependency-plugin/
