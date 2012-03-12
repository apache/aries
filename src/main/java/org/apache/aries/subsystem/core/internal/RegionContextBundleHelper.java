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
import org.osgi.framework.Version;

public class RegionContextBundleHelper {
	public static final String SYMBOLICNAME_PREFIX = Constants.RegionContextBundleSymbolicNamePrefix;
	public static final Version VERSION = Version.parseVersion("1.0.0");
	
	public static void installRegionContextBundle(AriesSubsystem subsystem) throws BundleException, IOException {
		String symbolicName = SYMBOLICNAME_PREFIX + subsystem.getSubsystemId();
		String location = subsystem.getLocation() + '/' + subsystem.getSubsystemId();
		Bundle b = subsystem.getRegion().getBundle(symbolicName, VERSION);
		if (b != null)
			return;
		ThreadLocalSubsystem.set(subsystem);
		b = subsystem.getRegion().installBundleAtLocation(location, createRegionContextBundle(symbolicName));
		// The region context bundle must be started persistently.
		b.start();
	}
	
	public static void uninstallRegionContextBundle(AriesSubsystem subsystem) {
		String symbolicName = SYMBOLICNAME_PREFIX + subsystem.getSubsystemId();
		Bundle bundle = subsystem.getRegion().getBundle(symbolicName, VERSION);
		if (bundle == null)
			throw new IllegalStateException("Missing region context bundle: " + symbolicName);
		try {
			bundle.uninstall();
		}
		catch (BundleException e) {
			// TODO Should we really eat this? At least log it?
		}
	}
	
	private static Manifest createManifest(String symbolicName) {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().putValue(Constants.BundleSymbolicName, symbolicName);
		manifest.getMainAttributes().putValue(Constants.BundleVersion, VERSION.toString());
		return manifest;
	}
	
	private static InputStream createRegionContextBundle(String symbolicName) throws IOException {
		Manifest manifest = createManifest(symbolicName);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JarOutputStream jos = new JarOutputStream(baos, manifest);
		jos.close();
		return new ByteArrayInputStream(baos.toByteArray());
	}
}
