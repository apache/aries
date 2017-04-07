/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

def fileP1 = new File(basedir, 'target/generated-sources/blueprint/OSGI-INF/blueprint/p1.xml')
assert fileP1.exists()
def xml1 = new groovy.util.XmlSlurper().parse(fileP1)
assert xml1."enable-annotations".size() == 0
assert xml1.bean.find{ it.@class == 'p1.T1'}.@id == 't1'
assert xml1.bean.transaction.find{ it.name() == 'transaction' }.size() == 1

def fileP2 = new File(basedir, 'target/generated-sources/blueprint/OSGI-INF/blueprint/p2.xml')
assert fileP2.exists()
def xml2 = new groovy.util.XmlSlurper().parse(fileP2)
assert xml2."enable-annotations".size() == 1

def fileP3 = new File(basedir, 'target/generated-sources/blueprint/OSGI-INF/blueprint/p3.xml')
assert fileP3.exists()
def xml3 = new groovy.util.XmlSlurper().parse(fileP3)
assert xml3."enable-annotations".size() == 0