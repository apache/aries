/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.osgi.service.cdi.annotations.Component;
import org.osgi.service.cdi.annotations.ServiceScope;

@Component(
	property = {
		"test.key.b1=test.value.b1",
		"test.key.b2=test.value.b2",

		"p.Boolean:Boolean=true",
		"p.Boolean.array:Boolean=true",
		"p.Boolean.array:Boolean=false",

		"p.Boolean.list:List<Boolean>=false",
		"p.Boolean.list:List<Boolean>=true",

		"p.Boolean.set:Set<Boolean>=true",
		"p.Boolean.set:Set<Boolean>=true",
		"p.Boolean.set:Set<Boolean>=false",

		"p.Byte:Byte=2",

		"p.Byte.array:Byte=2",
		"p.Byte.array:Byte=34",

		"p.Byte.list:List<Byte>=34",
		"p.Byte.list:List<Byte>=2",

		"p.Byte.set:Set<Byte>=34",
		"p.Byte.set:Set<Byte>=34",
		"p.Byte.set:Set<Byte>=2",

		"p.Character:Character=C",

		"p.Character.array:Character=C",
		"p.Character.array:Character=D",

		"p.Character.list:List<Character>=D",
		"p.Character.list:List<Character>=C",

		"p.Character.set:Set<Character>=D",
		"p.Character.set:Set<Character>=D",
		"p.Character.set:Set<Character>=C",

		"p.Double:Double=2.5",

		"p.Double.array:Double=2.5",
		"p.Double.array:Double=45.678",

		"p.Double.list:List<Double>=45.678",
		"p.Double.list:List<Double>=2.5",

		"p.Double.set:Set<Double>=45.678",
		"p.Double.set:Set<Double>=45.678",
		"p.Double.set:Set<Double>=2.5",

		"p.Float:Float=3.4",

		"p.Float.array:Float=3.4",
		"p.Float.array:Float=78.9",

		"p.Float.list:List<Float>=78.9",
		"p.Float.list:List<Float>=3.4",

		"p.Float.set:Set<Float>=78.9",
		"p.Float.set:Set<Float>=78.9",
		"p.Float.set:Set<Float>=3.4",

		"p.Integer:Integer=5",

		"p.Integer.array:Integer=5",
		"p.Integer.array:Integer=34567",

		"p.Integer.list:List<Integer>=34567",
		"p.Integer.list:List<Integer>=5",

		"p.Integer.set:Set<Integer>=34567",
		"p.Integer.set:Set<Integer>=34567",
		"p.Integer.set:Set<Integer>=5",

		"p.Long:Long=7",

		"p.Long.array:Long=7",
		"p.Long.array:Long=7789654",

		"p.Long.list:List<Long>=7789654",
		"p.Long.list:List<Long>=7",

		"p.Long.set:Set<Long>=7789654",
		"p.Long.set:Set<Long>=7789654",
		"p.Long.set:Set<Long>=7",

		"p.Short:Short=25",

		"p.Short.array:Short=25",
		"p.Short.array:Short=196",

		"p.Short.list:List<Short>=196",
		"p.Short.list:List<Short>=25",

		"p.Short.set:Set<Short>=196",
		"p.Short.set:Set<Short>=196",
		"p.Short.set:Set<Short>=25",

		"p.String=black",

		"p.String.array=black",
		"p.String.array=green",

		"p.String.list:List<String>=green",
		"p.String.list:List<String>=black",

		"p.String.set:Set<String>=green",
		"p.String.set:Set<String>=green",
		"p.String.set:Set<String>=black"
	},
	service = {ServiceWithProperties.class, BeanService.class},
	serviceScope = ServiceScope.SINGLETON
)
@MoreProperties(glub_integer = 45, goo_string = "green")
public class ServiceWithProperties implements BeanService<Pojo> {

	@Qualifier
	@Retention(value = RetentionPolicy.RUNTIME)
	@Target(value = { ElementType.TYPE })
	public @interface MoreProperties {
		String goo_string();
		int glub_integer();
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
