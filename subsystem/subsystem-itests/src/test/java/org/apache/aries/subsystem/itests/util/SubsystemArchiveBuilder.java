package org.apache.aries.subsystem.itests.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.service.subsystem.SubsystemConstants;

import aQute.bnd.osgi.Constants;

public class SubsystemArchiveBuilder {
	public static final String ESA_EXTENSION = ".esa";
	public static final String JAR_EXTENSION = ".jar";
	public static final String SUBSYSTEM_MANIFEST_FILE = "OSGI-INF/SUBSYSTEM.MF";
	
	private final TinyBundle bundle;
	private final Manifest manifest;
	
	public SubsystemArchiveBuilder() {
		bundle = TinyBundles.bundle();
		manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
	}
	
	public InputStream build() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		manifest.write(baos);
		baos.close();
		return bundle
				.add(SUBSYSTEM_MANIFEST_FILE, new ByteArrayInputStream(baos.toByteArray()))
				.build();
	}
	
	public SubsystemArchiveBuilder bundle(String name, InputStream value) {
		return file(name + JAR_EXTENSION, value);
	}
	
	public SubsystemArchiveBuilder content(String value) {
		return header(SubsystemConstants.SUBSYSTEM_CONTENT, value);
	}
	
	public SubsystemArchiveBuilder exportPackage(String value) {
		return header(Constants.EXPORT_PACKAGE, value);
	}
	
	public SubsystemArchiveBuilder file(String name, InputStream value) {
		bundle.add(name, value);
		return this;
	}
	
	public SubsystemArchiveBuilder header(String name, String value) {
		manifest.getMainAttributes().putValue(name, value);
		return this;
	}
	
	public SubsystemArchiveBuilder importPackage(String value) {
		return header(Constants.IMPORT_PACKAGE, value);
	}
	
	public SubsystemArchiveBuilder provideCapability(String value) {
		return header(Constants.PROVIDE_CAPABILITY, value);
	}
	
	public SubsystemArchiveBuilder subsystem(String name, InputStream value) {
		return file(name + ESA_EXTENSION, value);
	}
	
	public SubsystemArchiveBuilder symbolicName(String value) {
		return header(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, value);
	}
	
	public SubsystemArchiveBuilder type(String value) {
		return header(SubsystemConstants.SUBSYSTEM_TYPE, value);
	}
}
