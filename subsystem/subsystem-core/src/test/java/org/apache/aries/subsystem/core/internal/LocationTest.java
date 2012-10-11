package org.apache.aries.subsystem.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;

import org.junit.Test;

public class LocationTest {
	@Test
	public void testAnyLocationString() {
		String locationStr = "anyLocation";
		Location location = null;
		try {
			location = new Location(locationStr);
		}
		catch (Throwable t) {
			t.printStackTrace();
			fail("Any location string must be supported");
		}
		assertNull("Wrong symbolic name", location.getSymbolicName());
		assertEquals("Wrong value", locationStr, location.getValue());
		assertNull("Wrong version", location.getVersion());
		try {
			location.open();
			fail("Opening a location that does not represent a URL should fail");
		}
		catch (MalformedURLException e) {
			// Okay
		}
		catch (Throwable t) {
			t.printStackTrace();
			fail("Wrong exception");
		}
	}
}
