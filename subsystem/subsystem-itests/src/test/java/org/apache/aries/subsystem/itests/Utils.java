package org.apache.aries.subsystem.itests;

import java.io.Closeable;

import org.osgi.framework.ServiceRegistration;

public class Utils {
	public static void closeQuietly(Closeable closeable) {
		if (closeable == null) return;
		try {
			closeable.close();
		}
		catch (Exception e) {}
	}
	
	public static void unregisterQuietly(ServiceRegistration<?> reg) {
		if (reg == null) return;
		try {
			reg.unregister();
		}
		catch (Exception e) {}
	}
}
