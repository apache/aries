package org.apache.aries.cdi.test.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.aries.cdi.test.interfaces.BundleScoped;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceFilterQualifier;

public class BeanWithReference {

	@Qualifier
	@ReferenceFilterQualifier
	@Retention(value = RetentionPolicy.RUNTIME)
	@Target(value = { ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR })
	public @interface ComplexEnoughKey {
		String value();
	}

	@Qualifier
	@ReferenceFilterQualifier
	@Retention(value = RetentionPolicy.RUNTIME)
	@Target(value = { ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR })
	public @interface ComplexAnnotation {
		String feeFi();
		int foFum();
	}

	@ComplexEnoughKey("fum")
	@ComplexAnnotation(feeFi = "fee", foFum = 23)
	@Inject
	@Reference
	BundleScoped bundleScoped;

}
