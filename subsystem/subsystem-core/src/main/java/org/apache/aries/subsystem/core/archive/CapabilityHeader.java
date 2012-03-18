package org.apache.aries.subsystem.core.archive;

import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public interface CapabilityHeader<C extends Clause> extends Header<C> {
	List<? extends Capability> toCapabilities(Resource resource);
}
