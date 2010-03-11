setlocal 
cd %~dp0
copy ..\source\org.apache.aries.tutorials.blueprint.greeter.api\target\*.jar ..\dropins

copy ..\source\org.apache.aries.tutorials.blueprint.greeter.server.osgi\target\*.jar ..\dropins

copy ..\source\org.apache.aries.tutorials.blueprint.greeter.client.osgi\target\*.jar ..\dropins
