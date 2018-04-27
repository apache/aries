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

package org.apache.aries.cdi.container.internal.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.aries.cdi.container.internal.util.Maps;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.cdi.annotations.BeanPropertyType;
import org.osgi.service.cdi.annotations.Service;

public class MapsTest {

	@Retention(RetentionPolicy.RUNTIME)
	@BeanPropertyType
	public @interface CPT1 {
		String[] a();
		int[] b();
		boolean c();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@BeanPropertyType
	public @interface CPT2 {
		String a();
		int[] b();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@BeanPropertyType
	public @interface CPT3 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@BeanPropertyType
	public @interface CPT4 {
		String b();
	}

	@CPT1(a = {"foo", "bar"}, b = {1, 1, 1}, c = true)
	@CPT2(a = "baz", b = {2, 2})
	@CPT3
	@Service(Integer.class)
	public Integer one;

	@Test
	public void checkMerge_1() throws Exception {
		Map<String, ?> merged = Maps.merge(
			Stream.of(getClass().getField("one").getAnnotations()).collect(Collectors.toList()));

		Assert.assertEquals(
			Maps.of(
				"a", Arrays.asList("foo", "bar", "baz"),
				"cpt3", true,
				"b", Arrays.asList(1,1,1,2,2),
				"c", true),
			merged);
	}

	@CPT1(a = {"foo", "bar"}, b = {1, 1, 1}, c = true)
	@CPT2(a = "baz", b = {2, 2})
	@CPT3
	@CPT4(b = "blah")
	@Service(Integer.class)
	public Integer two;

	@Test(expected = ClassCastException.class)
	public void checkMerge_2() throws Exception {
		Maps.merge(Stream.of(getClass().getField("two").getAnnotations()).collect(Collectors.toList()));
	}

}
