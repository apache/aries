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

def p1 = new File(basedir, 'target/generated-sources/blueprint/OSGI-INF/blueprint/p1.xml')
assert p1.exists()
def xmlP1 = new groovy.util.XmlSlurper().parse(p1)
assert xmlP1.name() == 'blueprint'
assert xmlP1.'@default-activation' == 'lazy'
assert xmlP1.bean.find { it.@class == 'p2.T2' }.size() == 0
assert xmlP1.bean.@class == 'p1.T1'
assert xmlP1.bean.@id == 't1'

def p2 = new File(basedir, 'target/generated-sources/blueprint/OSGI-INF/blueprint/p2.xml')
assert p2.exists()
def xmlP2 = new groovy.util.XmlSlurper().parse(p2)
assert xmlP2.name() == 'blueprint'
assert xmlP2.'@default-activation' == 'eager'
assert xmlP2.bean.find { it.@class == 'p1.T1' }.size() == 0
assert xmlP2.bean.@class == 'p2.T2'
assert xmlP2.bean.@id == 't2'