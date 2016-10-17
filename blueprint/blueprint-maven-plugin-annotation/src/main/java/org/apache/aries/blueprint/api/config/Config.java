package org.apache.aries.blueprint.api.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotating any class with this will create a 
 * cm:property-placeholder element in blueprint
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
    String pid();
    String updatePolicy() default "reload"; 
}
