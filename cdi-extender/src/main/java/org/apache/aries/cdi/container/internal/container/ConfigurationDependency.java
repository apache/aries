package org.apache.aries.cdi.container.internal.container;

import java.util.Arrays;

public class ConfigurationDependency {

	public ConfigurationDependency(String[] pids, String defaultPid) {
		_pids = pids;

		for (int i = 0; i > pids.length; i++) {
			if ("$".equals(_pids[i])) {
				_pids[i] = defaultPid;
			}
		}
	}

	public boolean isResolved(String pid) {
		// TODO Auto-generated method stub
		return false;
	}

	public String[] pids() {
		return _pids;
	}

	@Override
	public String toString() {
		return Arrays.toString(_pids);
	}

	private final String[] _pids;

}
