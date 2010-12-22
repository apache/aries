package org.apache.aries.spifly;

import org.osgi.framework.Version;

class BundleDescriptor {
    final String symbolicName;
    final Version version;
    
    BundleDescriptor(String symbolicName) {
        this(symbolicName, null);
    }

    BundleDescriptor(String symbolicName, Version version) {
        this.symbolicName = symbolicName;
        this.version = version;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public Version getVersion() {
        return version;
    }
}
