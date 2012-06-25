package org.apache.aries.subsystem.core.internal;

import java.util.Comparator;

import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.osgi.resource.Resource;

public class StartResourceComparator implements Comparator<Resource> {
	private final SubsystemContentHeader header;
	
	public StartResourceComparator(SubsystemContentHeader header) {
		this.header = header;
	}
	@Override
	public int compare(Resource r1, Resource r2) {
		Integer r1StartOrder = getStartOrder(r1);
		Integer r2StartOrder = getStartOrder(r2);
		return r1StartOrder.compareTo(r2StartOrder);
	}
	
	private Integer getStartOrder(Resource r) {
		SubsystemContentHeader.Clause clause = header.getClause(r);
		if (clause == null)
			return 0;
		return clause.getStartOrder();
	}
}
