package org.apache.aries.cdi.container.internal.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.aries.cdi.container.internal.util.Maps;
import org.junit.Test;

public class MapsTest {

	@Test
	public void testSingleRawStringConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo=bar"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals("bar", entry.getValue());
	}

	@Test
	public void testSingleStringConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:String=bar"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals("bar", entry.getValue());
	}

	@Test
	public void testSingleListStringConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:List<String>=bar"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(Collections.singletonList("bar"), entry.getValue());
	}

	@Test
	public void testSingleSetStringConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Set<String>=bar"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(Collections.singleton("bar"), entry.getValue());
	}

	@Test
	public void testRawStringArrayConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo=bar", "foo=baz"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertArrayEquals(new String[] {"bar", "baz"}, (String[])entry.getValue());
	}

	@Test
	public void testRawStringArrayConversion2() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo=bar", "foo=baz", "foo=fee"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertArrayEquals(new String[] {"bar", "baz", "fee"}, (String[])entry.getValue());
	}

	@Test
	public void testStringArrayConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:String=bar", "foo:String=baz"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertArrayEquals(new String[] {"bar", "baz"}, (String[])entry.getValue());
	}

	@Test
	public void testStringArrayConversion2() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:String=bar", "foo:String=baz", "foo:String=fee"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertArrayEquals(new String[] {"bar", "baz", "fee"}, (String[])entry.getValue());
	}

	@Test
	public void testListStringConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:List<String>=bar", "foo:List<String>=baz"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(Arrays.asList("bar", "baz"), entry.getValue());
	}

	@Test
	public void testListStringConversion2() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:List<String>=bar", "foo:List<String>=baz", "foo:List<String>=fee"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(Arrays.asList("bar", "baz", "fee"), entry.getValue());
	}

	@Test
	public void testSetStringConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Set<String>=bar", "foo:Set<String>=baz"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(new HashSet<String>(Arrays.asList("bar", "baz")), entry.getValue());
	}

	@Test
	public void testSetStringConversion2() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Set<String>=bar", "foo:Set<String>=baz", "foo:Set<String>=fee"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(new HashSet<String>(Arrays.asList("bar", "baz", "fee")), entry.getValue());
	}

	@Test
	public void testSingleBooleanConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Boolean=bar"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(false, entry.getValue());
	}

	@Test
	public void testSingleBooleanConversion2() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Boolean=true"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(true, entry.getValue());
	}

	@Test
	public void testSingleBooleanConversion3() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Boolean=0"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(false, entry.getValue());
	}

	@Test
	public void testArrayBooleanConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Boolean=bar", "foo:Boolean=false", "foo:Boolean=true"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertArrayEquals(new Boolean[] {false, false, true}, (Boolean[])entry.getValue());
	}

	@Test
	public void testListBooleanConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:List<Boolean>=true", "foo:List<Boolean>=bar", "foo:List<Boolean>=false"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(Arrays.asList(true, false, false), entry.getValue());
	}

	@Test
	public void testSetBooleanConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Set<Boolean>=0", "foo:Set<Boolean>=true", "foo:Set<Boolean>=false"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(new HashSet<Boolean>(Arrays.asList(false, true)), entry.getValue());
	}

	@Test
	public void testSingleByteConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Byte=1"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(new Byte("1"), entry.getValue());
	}

	@Test
	public void testSingleByteConversion2() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Byte=126"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(new Byte("126"), entry.getValue());
	}

	@Test
	public void testArrayByteConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Byte=1", "foo:Byte=96"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertArrayEquals(new Byte[] {new Byte("1"), new Byte("96")}, (Byte[])entry.getValue());
	}

	@Test
	public void testListByteConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:List<Byte>=126", "foo:List<Byte>=91"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(Arrays.asList(new Byte("126"), new Byte("91")), entry.getValue());
	}

	@Test
	public void testSetByteConversion() throws Exception {
		Set<Entry<String,Object>> set = Maps.map(new String[] {"foo:Set<Byte>=126", "foo:Set<Byte>=91", "foo:Set<Byte>=91"}).entrySet();

		Entry<String, Object> entry = set.iterator().next();
		assertEquals("foo", entry.getKey());
		assertEquals(new HashSet<Byte>(Arrays.asList(new Byte("126"), new Byte("91"))), entry.getValue());
	}

	@Test
	public void testMixedConversion() throws Exception {
		Map<String,Object> map = Maps.map(
			new String[] {
				"foo:Set<Byte>=126", "foo:Set<Byte>=91", "foo:Set<Byte>=91",
				"fum=blaz", "fee:List<Double>=91.8765", "fee:List<Double>=34567.8965"});

		assertEquals(3, map.size());
		assertTrue(map.containsKey("foo"));
		assertTrue(map.containsKey("fum"));
		assertTrue(map.containsKey("fee"));
		assertTrue(map.get("foo") instanceof Set);
		assertTrue(map.get("fum") instanceof String);
		assertTrue(map.get("fee") instanceof List);
		assertEquals(2, ((Set)map.get("foo")).size());
		assertEquals("blaz", map.get("fum"));
		assertEquals(2, ((List)map.get("fee")).size());
	}

}
