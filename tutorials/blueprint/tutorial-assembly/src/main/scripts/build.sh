#!/bin/sh
scriptDir=`pwd`/`dirname $0`
project=$1

if [ "${project}" != "NONE" ]; then
  echo Building project ${project}
  cd "../source/${project}"
  mvn clean install
fi
