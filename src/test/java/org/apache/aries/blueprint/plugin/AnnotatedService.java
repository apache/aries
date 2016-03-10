package org.apache.aries.blueprint.plugin;

import javax.inject.Named;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@Named("annotatedService")
public @interface AnnotatedService {
}
