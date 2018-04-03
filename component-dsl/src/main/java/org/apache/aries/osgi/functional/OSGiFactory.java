package org.apache.aries.osgi.functional;

/**
 * @author Carlos Sierra Andrés
 */
public interface OSGiFactory {

    <T> OSGi<T> create(OSGiRunnable<T> osgiRunnable);

}
