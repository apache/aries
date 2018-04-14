package org.apache.aries.cdi.container.internal.component;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static org.apache.aries.cdi.container.internal.component.DiscoveryExtension.checkIfBeanClassIsOSGiAnnotated;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.FactoryComponent;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;

public class IfBeanClassIsOSGiAnnotatedTest {

	class Bar {}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({CONSTRUCTOR, FIELD, METHOD, PARAMETER, TYPE, TYPE_USE})
	@interface Anno {}

	@Anno
	@SuppressWarnings("serial")
	class Fum extends @Anno Bar implements @Anno Serializable {
		@Anno Fum(@Anno String param) {}
		@Anno String field;
		@Anno String method(@Anno String param) {return param;}
	}

	@Test
	public void testNotAnnotated() {
		assertFalse(checkIfBeanClassIsOSGiAnnotated(Fum.class));
	}

	@Test
	public void testFactoryComponent() throws Exception {
		@FactoryComponent
		class Foo {}

		assertTrue(checkIfBeanClassIsOSGiAnnotated(Foo.class));
	}

	@Test
	public void testSingleComponent() throws Exception {
		@SingleComponent
		class Foo {}

		assertTrue(checkIfBeanClassIsOSGiAnnotated(Foo.class));
	}

	@Test
	public void testComponentScoped() throws Exception {
		class FooField { @ComponentScoped String field;}
		class FooMethod { @ComponentScoped void method() {}}
		@ComponentScoped
		class FooType {}

		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooField.class));
		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooMethod.class));
		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooType.class));
	}

	@Test
	public void testConfiguration() throws Exception {
		class Foo { @SuppressWarnings("unused")
		Foo(@Configuration String param) {}}
		class FooField { @Configuration String field;}
		class FooMethod { @SuppressWarnings("unused")
		void method(@Configuration String param) {}}

		assertTrue(checkIfBeanClassIsOSGiAnnotated(Foo.class));
		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooField.class));
		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooMethod.class));
	}

	@Test
	public void testReference() throws Exception {
		class Foo { @SuppressWarnings("unused")
		Foo(@Reference String param) {}}
		class FooField { @Reference String field;}
		class FooMethod { @SuppressWarnings("unused")
		void method(@Reference String param) {}}

		assertTrue(checkIfBeanClassIsOSGiAnnotated(Foo.class));
		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooField.class));
		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooMethod.class));
	}

	@Test
	public void testService() throws Exception {
		@Service
		class Foo {}
		@SuppressWarnings("serial")
		class FooImplements implements @Service Serializable {}
		class FooExtends extends @Service Foo {}
		class FooField { @Service String field;}
		class FooMethod { @Service void method() {}}

		assertTrue(checkIfBeanClassIsOSGiAnnotated(Foo.class));
		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooImplements.class));
		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooExtends.class));
		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooField.class));
		assertTrue(checkIfBeanClassIsOSGiAnnotated(FooMethod.class));
	}
}
