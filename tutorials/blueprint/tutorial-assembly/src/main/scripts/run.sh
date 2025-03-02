#!/bin/sh
################################################################################
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
################################################################################
scriptDir=`dirname $0`
binDir="${scriptDir}/../bin"
targetDir="${scriptDir}/../dropins"
mkdir $targetDir
cp "${binDir}/org.apache.aries.tutorials.blueprint.greeter.api-0.1-SNAPSHOT.jar" $targetDir
sleep 3
cp "${binDir}/org.apache.aries.tutorials.blueprint.greeter.server.osgi-0.1-SNAPSHOT.jar" $targetDir
sleep 3
cp "${binDir}/org.apache.aries.tutorials.blueprint.greeter.server.blueprint-0.1-SNAPSHOT.jar" $targetDir
sleep 3
cp "${binDir}/org.apache.aries.tutorials.blueprint.greeter.client.osgi-0.1-SNAPSHOT.jar" $targetDir
sleep 3
cp "${binDir}/org.apache.aries.tutorials.blueprint.greeter.client.blueprint-0.1-SNAPSHOT.jar" $targetDir
