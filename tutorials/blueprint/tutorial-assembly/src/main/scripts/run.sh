#!/bin/sh
scriptDir=`dirname $0`
cp "${scriptDir}/../source/org.apache.aries.tutorials.blueprint.greeter.api/target/"*.jar "${scriptDir}/../dropins"
cp "${scriptDir}/../source/org.apache.aries.tutorials.blueprint.greeter.server.osgi/target/"*.jar "${scriptDir}/../dropins"
cp "${scriptDir}/../source/org.apache.aries.tutorials.blueprint.greeter.client.osgi/target/"*.jar "${scriptDir}/../dropins"
