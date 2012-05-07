package org.apache.aries.subsystem.core.internal;

import org.osgi.resource.Requirement;

public abstract class AbstractRequirement implements Requirement {
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Requirement))
			return false;
		Requirement c = (Requirement)o;
		return c.getNamespace().equals(getNamespace())
				&& c.getAttributes().equals(getAttributes())
				&& c.getDirectives().equals(getDirectives())
				&& c.getResource() != null ? c.getResource().equals(
				getResource()) : getResource() == null;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + getNamespace().hashCode();
		result = 31 * result + getAttributes().hashCode();
		result = 31 * result + getDirectives().hashCode();
		result = 31 * result
				+ (getResource() == null ? 0 : getResource().hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getName()).append(": ")
				.append("namespace=").append(getNamespace())
				.append(", attributes=").append(getAttributes())
				.append(", directives=").append(getDirectives())
				.append(", resource=").append(getResource()).toString();
	}
}
