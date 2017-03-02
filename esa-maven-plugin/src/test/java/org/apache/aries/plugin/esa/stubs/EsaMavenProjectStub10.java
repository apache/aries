package org.apache.aries.plugin.esa.stubs;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.LinkedHashSet;
import java.util.Set;

public class EsaMavenProjectStub10 extends EsaMavenProjectStub {

	@Override
	public Set getArtifacts()
	{
		Set artifacts = new LinkedHashSet();

		artifacts.add( createArtifact( "org.apache.maven.test", "maven-artifact01", "1.0-SNAPSHOT", false ) );
		artifacts.add( createArtifact( "org.apache.maven.test", "maven-artifact02", "1.0-SNAPSHOT", false ) );
		artifacts.add( createArtifact( "org.apache.maven.test", "maven-artifact03", "1.1-SNAPSHOT", false ) );


		return artifacts;
	}

	@Override
	public Set getDependencyArtifacts() {
		Set artifacts = new LinkedHashSet();

		artifacts.add( createArtifact( "org.apache.maven.test", "maven-artifact01", "1.0-SNAPSHOT", false ) );
		artifacts.add( createArtifact( "org.apache.maven.test", "maven-artifact02", "1.0-SNAPSHOT", false ) );

		return artifacts;
	}
}
