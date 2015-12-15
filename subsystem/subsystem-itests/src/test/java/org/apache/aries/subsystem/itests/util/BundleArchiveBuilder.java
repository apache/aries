package org.apache.aries.subsystem.itests.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.ops4j.pax.tinybundles.core.InnerClassStrategy;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;

import aQute.bnd.osgi.Constants;

public class BundleArchiveBuilder {
	public static final String JAR_EXTENSION = ".jar";
	
	private final TinyBundle bundle;
	
	public BundleArchiveBuilder() {
		bundle = TinyBundles.bundle();
	}
	
	public BundleArchiveBuilder activator(Class<?> clazz) {
		bundle.set(Constants.BUNDLE_ACTIVATOR, clazz.getName());
		bundle.add(clazz, InnerClassStrategy.NONE);
		return this;
	}
	
	public InputStream build() {
		return bundle.build();
	}
	
	public byte[] buildAsBytes() throws IOException {
		InputStream is = build();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] bytes = new byte[2048];
		int count;
		while ((count = is.read(bytes)) != -1) {
			baos.write(bytes, 0, count);
		}
		is.close();
		baos.close();
		return baos.toByteArray();
	}
	
	public BundleArchiveBuilder clazz(Class<?> clazz) {
		bundle.add(clazz, InnerClassStrategy.NONE);
		return this;
	}
	
	public BundleArchiveBuilder exportPackage(String value) {
		return header(Constants.EXPORT_PACKAGE, value);
	}
	
	public BundleArchiveBuilder header(String name, String value) {
		bundle.set(name, value);
		return this;
	}
	
	public BundleArchiveBuilder importPackage(String value) {
		return header(Constants.IMPORT_PACKAGE, value);
	}
	
	public BundleArchiveBuilder provideCapability(String value) {
		return header(Constants.PROVIDE_CAPABILITY, value);
	}
	
	public BundleArchiveBuilder requireBundle(String value) {
		return header(Constants.REQUIRE_BUNDLE, value);
	}
	
	public BundleArchiveBuilder requireCapability(String value) {
		return header(Constants.REQUIRE_CAPABILITY, value);
	}
	
	public BundleArchiveBuilder symbolicName(String value) {
		return header(Constants.BUNDLE_SYMBOLICNAME, value);
	}
}
