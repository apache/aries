The Subsystems subproject uses RFC 138(Framework hooks).
You need to download the implementation jar at:
    http://download.eclipse.org/equinox/
You need to copy it in your local m2 repository:
  ~/.m2/repository/org/eclipse/osgi/3.7.0.v20100910/osgi-3.7.0.v20100910.jar

I tested using the 3.7M2 stable builds.

Also, you need to run the following command to index your local m2 repo to ~/.m2/repository/repository.xml file. 

mvn org.apache.felix:maven-bundle-plugin:2.1.0:index  -DurlTemplate=maven
