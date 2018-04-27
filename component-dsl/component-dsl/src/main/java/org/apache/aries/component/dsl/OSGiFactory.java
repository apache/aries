package org.apache.aries.component.dsl;

/**
 * @author Carlos Sierra Andrés
 */
public interface OSGiFactory {

    <T> OSGi<T> create(OSGiRunnable<T> osgiRunnable);

}
