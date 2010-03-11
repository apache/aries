@echo off
setlocal 
cd %~dp0

echo Building project %1
cd ../source/%1
mvn clean install
