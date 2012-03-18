package org.apache.aries.subsystem.core.archive;

import java.util.List;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public interface RequirementHeader<C extends Clause> extends Header<C> {
	List<? extends Requirement> toRequirements(Resource resource);
}
