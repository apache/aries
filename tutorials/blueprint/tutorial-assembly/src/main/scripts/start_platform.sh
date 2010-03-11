#!/bin/sh
scriptDir=`pwd`/`dirname $0`
cd "${scriptDir}/../platform"
java -jar osgi-3.5.0.v20090520.jar -console -clean
