package org.apache.aries.cdi.test.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.apache.aries.cdi.test.beans.ServiceWithProperties.MoreProperties;
import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.PropertyType;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.ServiceProperty;
import org.osgi.service.cdi.annotations.ServicePropertyQualifier;

@Service(
	properties = {
		@ServiceProperty(key = "test.key.b1", value = "test.value.b1"),
		@ServiceProperty(key = "test.key.b2", value = "test.value.b2"),

		@ServiceProperty(key = "p.Boolean", value = "true", type = PropertyType.Boolean),
		@ServiceProperty(key = "p.Boolean.array", value = "true,false", type = PropertyType.Boolean_Array),
		@ServiceProperty(key = "p.Boolean.list", value = "false,true", type = PropertyType.Boolean_List),
		@ServiceProperty(key = "p.Boolean.set", value = "true, true, false", type = PropertyType.Boolean_Set),
		@ServiceProperty(key = "p.Byte", value = "2", type = PropertyType.Byte),
		@ServiceProperty(key = "p.Byte.array", value = "2,34", type = PropertyType.Byte_Array),
		@ServiceProperty(key = "p.Byte.list", value = "34,2", type = PropertyType.Byte_List),
		@ServiceProperty(key = "p.Byte.set", value = "34,34,2", type = PropertyType.Byte_Set),
		@ServiceProperty(key = "p.Character", value = "C", type = PropertyType.Character),
		@ServiceProperty(key = "p.Character.array", value = "C,D", type = PropertyType.Character_Array),
		@ServiceProperty(key = "p.Character.list", value = "D,C", type = PropertyType.Character_List),
		@ServiceProperty(key = "p.Character.set", value = "D,D,C", type = PropertyType.Character_Set),
		@ServiceProperty(key = "p.Double", value = "2.5", type = PropertyType.Double),
		@ServiceProperty(key = "p.Double.array", value = "2.5,45.678", type = PropertyType.Double_Array),
		@ServiceProperty(key = "p.Double.list", value = "45.678,2.5", type = PropertyType.Double_List),
		@ServiceProperty(key = "p.Double.set", value = "45.678,45.678,2.5", type = PropertyType.Double_Set),
		@ServiceProperty(key = "p.Float", value = "3.4", type = PropertyType.Float),
		@ServiceProperty(key = "p.Float.array", value = "3.4,78.9", type = PropertyType.Float_Array),
		@ServiceProperty(key = "p.Float.list", value = "78.9,3.4", type = PropertyType.Float_List),
		@ServiceProperty(key = "p.Float.set", value = "78.9,78.9,3.4", type = PropertyType.Float_Set),
		@ServiceProperty(key = "p.Integer", value = "5", type = PropertyType.Integer),
		@ServiceProperty(key = "p.Integer.array", value = "5,34567", type = PropertyType.Integer_Array),
		@ServiceProperty(key = "p.Integer.list", value = "34567,5", type = PropertyType.Integer_List),
		@ServiceProperty(key = "p.Integer.set", value = "34567,34567,5", type = PropertyType.Integer_Set),
		@ServiceProperty(key = "p.Long", value = "7", type = PropertyType.Long),
		@ServiceProperty(key = "p.Long.array", value = "7,7789654", type = PropertyType.Long_Array),
		@ServiceProperty(key = "p.Long.list", value = "7789654,7", type = PropertyType.Long_List),
		@ServiceProperty(key = "p.Long.set", value = "7789654,7789654,7", type = PropertyType.Long_Set),
		@ServiceProperty(key = "p.Short", value = "25", type = PropertyType.Short),
		@ServiceProperty(key = "p.Short.array", value = "25,196", type = PropertyType.Short_Array),
		@ServiceProperty(key = "p.Short.list", value = "196,25", type = PropertyType.Short_List),
		@ServiceProperty(key = "p.Short.set", value = "196,196,25", type = PropertyType.Short_Set),
		@ServiceProperty(key = "p.String", value = "black", type = PropertyType.String),
		@ServiceProperty(key = "p.String.array", value = "black,green", type = PropertyType.String_Array),
		@ServiceProperty(key = "p.String.list", value = "green,black", type = PropertyType.String_List),
		@ServiceProperty(key = "p.String.set", value = "green,green,black", type = PropertyType.String_Set)
	},
	type = {ServiceWithProperties.class, BeanService.class}
)
@Singleton
@MoreProperties(glubInteger = 45, gooString = "green")
public class ServiceWithProperties implements BeanService<Pojo> {

	@Qualifier
	@ServicePropertyQualifier
	@Retention(value = RetentionPolicy.RUNTIME)
	@Target(value = { ElementType.TYPE })
	public @interface MoreProperties {
		String gooString();
		int glubInteger();
	}

	@Override
	public String doSomething() {
		return _pojo.foo("FIELD");
	}

	@Override
	public org.apache.aries.cdi.test.interfaces.Pojo get() {
		return _pojo;
	}

	@Inject
	private PojoImpl _pojo;

}
