setlocal 
cd %~dp0
copy ..\source\greeter-api\target\*.jar ..\dropins

copy ..\source\greeter-server-osgi\target\*.jar ..\dropins

copy ..\source\greeter-client-osgi\target\*.jar ..\dropins
