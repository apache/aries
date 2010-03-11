#!/bin/sh
scriptDir=`dirname $0`
cp "${scriptDir}/../source/greeter-api/target/"*.jar "${scriptDir}/../dropins"
cp "${scriptDir}/../source/greeter-server-osgi/target/"*.jar "${scriptDir}/../dropins"
cp "${scriptDir}/../source/greeter-client-osgi/target/"*.jar "${scriptDir}/../dropins"
