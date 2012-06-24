package org.apache.aries.subsystem.core.internal;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.aries.subsystem.core.archive.Header;

public class GetSubsystemHeadersAction implements PrivilegedAction<Map<String, String>> {
	private final AriesSubsystem subsystem;
	
	public GetSubsystemHeadersAction(AriesSubsystem subsystem) {
		this.subsystem = subsystem;
	}
	
	@Override
	public Map<String, String> run() {
		Map<String, Header<?>> headers = subsystem.getSubsystemManifest().getHeaders();
		Map<String, String> result = new HashMap<String, String>(headers.size());
		for (Entry<String, Header<?>> entry: headers.entrySet()) {
			Header<?> value = entry.getValue();
			result.put(entry.getKey(), value.getValue());
		}
		return result;
	}

}
