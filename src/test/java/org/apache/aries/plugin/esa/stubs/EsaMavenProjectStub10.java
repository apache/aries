package org.apache.aries.plugin.esa.stubs;

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
