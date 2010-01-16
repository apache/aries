################################################################################
# Copyright 2010 
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

Apache Aries Samples
----------------------------------------------------------------------

This sample demonstrates blueprint inlined managers usage.

This README uses the following aliases to describe directories.  These aliases should be replaced with your actual directory paths.
   %SAMPLE_HOME% refers to the root directory where Aries samples are extracted.
   

BUILD
-----
1. cd %SAMPLE_HOME%/blueprint-sample-idverifier
2. maven clean install
   

RUN
---
1. cd %SAMPLE_HOME%/blueprint-sample-idverifier/blueprint-sample-idverifier-assembly/target
2. run.bat|.sh

MEMO
----
After the sample is running, you can connect to it via JConsole to check the sample bundles via blueprint MBeans.
