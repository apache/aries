package org.apache.aries.subsystem.core.resource;

import org.osgi.resource.Capability;

public abstract class AbstractCapability implements Capability {
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Capability))
			return false;
		Capability c = (Capability)o;
		return c.getNamespace().equals(getNamespace())
				&& c.getAttributes().equals(getAttributes())
				&& c.getDirectives().equals(getDirectives())
				&& c.getResource().equals(getResource());
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + getNamespace().hashCode();
		result = 31 * result + getAttributes().hashCode();
		result = 31 * result + getDirectives().hashCode();
		result = 31 * result + getResource().hashCode();
		return result;
	}
	
	@Override
	public String toString() {
		return new StringBuffer().append("[Capability: ")
				.append("namespace=").append(getNamespace())
				.append(", attributes=").append(getAttributes())
				.append(", directives=").append(getDirectives())
				.append(", resource=").append(getResource()).append(']')
				.toString();
	}
}
