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

package org.apache.aries.cdi.container.internal.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.enterprise.util.TypeLiteral;

import org.jboss.weld.exceptions.IllegalArgumentException;

@SuppressWarnings("serial")
public enum PropertyType {

	Boolean("Boolean", Aggregate.RAW, new TypeLiteral<Boolean>() {}),
	Byte("Byte", Aggregate.RAW, new TypeLiteral<Byte>() {}),
	Character("Character", Aggregate.RAW, new TypeLiteral<Character>() {}),
	Double("Double", Aggregate.RAW, new TypeLiteral<Double>() {}),
	Float("Float", Aggregate.RAW, new TypeLiteral<Float>() {}),
	Integer("Integer", Aggregate.RAW, new TypeLiteral<Integer>() {}),
	Long("Long", Aggregate.RAW, new TypeLiteral<Long>() {}),
	Short("Short", Aggregate.RAW, new TypeLiteral<Short>() {}),
	String("String", Aggregate.RAW, new TypeLiteral<String>() {}),

	Boolean_Array("Boolean", Aggregate.ARRAY, new TypeLiteral<Boolean[]>() {}),
	Byte_Array("Byte", Aggregate.ARRAY, new TypeLiteral<Byte[]>() {}),
	Character_Array("Character", Aggregate.ARRAY, new TypeLiteral<Character[]>() {}),
	Double_Array("Double", Aggregate.ARRAY, new TypeLiteral<Double[]>() {}),
	Float_Array("Float", Aggregate.ARRAY, new TypeLiteral<Float[]>() {}),
	Integer_Array("Integer", Aggregate.ARRAY, new TypeLiteral<Integer[]>() {}),
	Long_Array("Long", Aggregate.ARRAY, new TypeLiteral<Long[]>() {}),
	Short_Array("Short", Aggregate.ARRAY, new TypeLiteral<Short[]>() {}),
	String_Array("String", Aggregate.ARRAY, new TypeLiteral<String[]>() {}),

	Boolean_List("Boolean", Aggregate.LIST, new TypeLiteral<List<Boolean>>() {}),
	Byte_List("Byte", Aggregate.LIST, new TypeLiteral<List<Byte>>() {}),
	Character_List("Character", Aggregate.LIST, new TypeLiteral<List<Character>>() {}),
	Double_List("Double", Aggregate.LIST, new TypeLiteral<List<Double>>() {}),
	Float_List("Float", Aggregate.LIST, new TypeLiteral<List<Float>>() {}),
	Integer_List("Integer", Aggregate.LIST, new TypeLiteral<List<Integer>>() {}),
	Long_List("Long", Aggregate.LIST, new TypeLiteral<List<Long>>() {}),
	Short_List("Short", Aggregate.LIST, new TypeLiteral<List<Short>>() {}),
	String_List("String", Aggregate.LIST, new TypeLiteral<List<String>>() {}),

	Boolean_Set("Boolean", Aggregate.SET, new TypeLiteral<Set<Boolean>>() {}),
	Byte_Set("Byte", Aggregate.SET, new TypeLiteral<Set<Byte>>() {}),
	Character_Set("Character", Aggregate.SET, new TypeLiteral<Set<Character>>() {}),
	Double_Set("Double", Aggregate.SET, new TypeLiteral<Set<Double>>() {}),
	Float_Set("Float", Aggregate.SET, new TypeLiteral<Set<Float>>() {}),
	Integer_Set("Integer", Aggregate.SET, new TypeLiteral<Set<Integer>>() {}),
	Long_Set("Long", Aggregate.SET, new TypeLiteral<Set<Long>>() {}),
	Short_Set("Short", Aggregate.SET, new TypeLiteral<Set<Short>>() {}),
	String_Set("String", Aggregate.SET, new TypeLiteral<Set<String>>() {});

	public static PropertyType arrayOf(String value) {
		PropertyType propertyType = valueOf(value);

		return valueOf(propertyType.raw + "_Array");
	}

	public static PropertyType find(String value) {
		for (PropertyType propertyType : values()) {
			if (propertyType.toString().equals(value))
				return propertyType;
		}

		throw new IllegalArgumentException("No such PropertyType: " + value);
	}

	PropertyType(String raw, Aggregate a, TypeLiteral<?> typeLiteral) {
		this.raw = raw;
		this.a = a;
		this.typeLiteral = typeLiteral;
	}

	public Class<?> componentType() {
		switch (a) {
			case RAW:
				return (Class<?>)getType();
			case ARRAY:
				return getType().getClass().getComponentType();
			default:
				ParameterizedType pt = (ParameterizedType)getType();
				return (Class<?>)pt.getActualTypeArguments()[0];
		}
	}

	public Type getType() {
		return typeLiteral.getType();
	}

	public boolean isArray() {
		return a == Aggregate.ARRAY;
	}

	public boolean isList() {
		return a == Aggregate.LIST;
	}

	public boolean isRaw() {
		return a == Aggregate.RAW;
	}

	public boolean isSet() {
		return a == Aggregate.SET;
	}

	@Override
	public java.lang.String toString() {
		switch (a) {
			case ARRAY:
				return raw + "[]";
			case LIST:
				return "List<" + raw + ">";
			case RAW:
				return raw;
			case SET:
				return "Set<" + raw + ">";
		}

		return raw;
	}

	private final Aggregate a;
	private final String raw;
	private final TypeLiteral<?> typeLiteral;

	enum Aggregate {
		ARRAY, LIST, SET, RAW
	}

}
