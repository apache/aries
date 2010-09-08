The Subsystems subproject uses RFC 138 which has not been published by the OSGi alliance yet.
The binaries are available from the OSGi repo if you are an OSGi alliance member.
You need to download the implementation jar at:
  https://www.osgi.org/members/svn/build/trunk/licensed/repo/org.eclipse.osgi.v43prototype
You need to copy it in your local m2 repository:
  ~/.m2/repository/org/eclipse/osgi/v43prototype-3.6.0.201003231329/osgi-v43prototype-3.6.0.201003231329.jar 

Also, you need to run the following command to index your local m2 repo to ~/.m2/repository/repository.xml file. 

mvn org.apache.felix:maven-bundle-plugin:2.1.0:index  -DurlTemplate=maven
