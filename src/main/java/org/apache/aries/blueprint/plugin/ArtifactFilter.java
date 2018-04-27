package org.apache.aries.blueprint.plugin;

import org.apache.maven.artifact.Artifact;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

class ArtifactFilter {

    private final Set<Pattern> includeArtifactPatterns;
    private final Set<Pattern> excludeArtifactPatterns;

    ArtifactFilter(Set<String> includeArtifacts, Set<String> excludeArtifacts) {
        includeArtifactPatterns = buildArtifactPatterns(includeArtifacts);
        excludeArtifactPatterns = buildPatterns(excludeArtifacts);
    }

    boolean shouldExclude(Artifact artifact) {
        return !canBeIncluded(artifact) || shouldBeExcluded(artifact);
    }

    private Set<Pattern> buildArtifactPatterns(Set<String> includeArtifacts) {
        if (includeArtifacts.isEmpty()) {
            Set<Pattern> patterns = new HashSet<>();
            patterns.add(Pattern.compile(".*"));
            return patterns;
        }
        return buildPatterns(includeArtifacts);
    }

    private Set<Pattern> buildPatterns(Set<String> artifactFilters) {
        Set<Pattern> artifactPatterns = new HashSet<>();
        for (String artifactFilter : artifactFilters) {
            artifactPatterns.add(Pattern.compile(artifactFilter));
        }
        return artifactPatterns;
    }

    private boolean shouldBeExcluded(Artifact artifact) {
        for (Pattern excludeArtifactPattern : excludeArtifactPatterns) {
            if (excludeArtifactPattern.matcher(artifact.toString()).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean canBeIncluded(Artifact artifact) {
        for (Pattern includeArtifactPattern : includeArtifactPatterns) {
            if (includeArtifactPattern.matcher(artifact.toString()).matches()) {
                return true;
            }
        }
        return false;
    }
}
