/*  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jpa.context.itest;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.apache.aries.itest.ExtraOptions.*;

@RunWith(JUnit4TestRunner.class)
public class EclipseLinkContextTest extends JPAContextTest {
    @Configuration
    public static Option[] eclipseLinkConfig() {
        return options(        
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.antlr"),
                
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.eclipselink.adapter"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle.eclipselink")
        );
    }
}
