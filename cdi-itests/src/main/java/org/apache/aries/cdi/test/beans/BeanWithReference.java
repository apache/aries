package org.apache.aries.cdi.test.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.aries.cdi.test.interfaces.BundleScoped;
import org.osgi.service.cdi.annotations.Reference;

public class BeanWithReference {

	@Qualifier
	@Retention(value = RetentionPolicy.RUNTIME)
	@Target(value = { ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR })
	public @interface ComplexEnoughKey {
		String complex_enough_key();
	}

	@Qualifier
	@Retention(value = RetentionPolicy.RUNTIME)
	@Target(value = { ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR })
	public @interface ComplexAnnotation {
		String fee_fi();
		int fo_fum();
	}

	@Qualifier
	@Retention(value = RetentionPolicy.RUNTIME)
	@Target(value = { ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR })
	public @interface SimpleAnnotation {
		String value();
	}

	@ComplexEnoughKey(complex_enough_key= "fum")
	@ComplexAnnotation(fee_fi = "fee", fo_fum = 23)
	@SimpleAnnotation("blah")
	@Inject
	@Reference
	@Default
	BundleScoped bundleScoped;

}
