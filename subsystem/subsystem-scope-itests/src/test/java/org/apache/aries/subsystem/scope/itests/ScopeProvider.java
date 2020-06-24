package org.apache.aries.subsystem.scope.itests;

import org.apache.aries.subsystem.scope.Scope;

public interface ScopeProvider {
//IC see: https://issues.apache.org/jira/browse/ARIES-594
	Scope getScope();
}
