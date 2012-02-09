package org.apache.aries.subsystem.core.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class RegionContextBundleHelper {
	public static final String SYMBOLICNAME_PREFIX = "org.osgi.service.subsystem.region.context.";
	public static final Version VERSION = Version.parseVersion("1.0.0");
	
	public static void installRegionContextBundle(AriesSubsystem subsystem) throws BundleException, IOException {
		String symbolicName = SYMBOLICNAME_PREFIX + subsystem.getSubsystemId();
		String location = subsystem.getLocation() + '/' + subsystem.getSubsystemId();
		Bundle b = subsystem.getRegion().getBundle(symbolicName, VERSION);
		if (b != null)
			return;
		Bundle t = subsystem.getRegion().installBundle(location + "/temp", createTempBundle(symbolicName));
		try {
			t.start();
			b = t.getBundleContext().installBundle(location, createRegionContextBundle(symbolicName));
		}
		finally {
			try {
				t.uninstall();
			}
			catch (BundleException e) {}
		}
		b.start(Bundle.START_TRANSIENT);
	}
	
	private static InputStream createRegionContextBundle(String symbolicName) throws IOException {
		Manifest manifest = createManifest(symbolicName);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JarOutputStream jos = new JarOutputStream(baos, manifest);
		jos.close();
		return new ByteArrayInputStream(baos.toByteArray());
	}
	
	private static Manifest createManifest(String symbolicName) {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		manifest.getMainAttributes().putValue(Constants.BUNDLE_VERSION, VERSION.toString());
		return manifest;
	}
	
	private static InputStream createTempBundle(String symbolicName) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, symbolicName + ".temp");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JarOutputStream jos = new JarOutputStream(baos, manifest);
		jos.close();
		return new ByteArrayInputStream(baos.toByteArray());
	}
}
