package org.apache.aries.blueprint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lists the namespaces supported by a given <code>NamespaceHandler</code>. 
 * <code>NamespaceHandler</code> implementations may optionally use this annotation to
 * simplify the auto-registration process in some deployment scenarios.     
 */
        
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Namespaces {
    /**
     * A list of namespaces supported by <code>NamespaceHandler</code>.
     */
    String[] value();
}
