package org.apache.aries.subsystem.core.internal;

import java.util.Comparator;
import java.util.List;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class InstallResourceComparator implements Comparator<Resource> {
	@Override
	public int compare(Resource r1, Resource r2) {
		String r1type = getResourceType(r1);
		String r2type = getResourceType(r2);
		if (r1type.equals(r2type))
			return 0;
		if (r1type.startsWith("osgi.subsystem"))
			return 1;
		return -1;
	}
	
	private String getResourceType(Resource r) {
		List<Capability> cl = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		Capability c = cl.get(0);
		Object o = c.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
		return String.valueOf(o);
	}
}
