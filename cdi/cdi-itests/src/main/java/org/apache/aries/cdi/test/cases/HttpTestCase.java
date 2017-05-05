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

package org.apache.aries.cdi.test.cases;

import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.util.tracker.ServiceTracker;

public class HttpTestCase extends AbstractTestCase {

	public void testSessionBean() throws Exception {
		Bundle tb5Bundle = installBundle("tb6.jar");

		try {
			String path = "/foo";

			RequestInfoDTO requestInfoDTO = waitFor(path);

			assertEquals("foo", requestInfoDTO.servletDTO.name);

			HttpClientBuilder clientBuilder = hcbf.newBuilder();
			CloseableHttpClient httpclient = clientBuilder.build();

			CookieStore cookieStore = new BasicCookieStore();
			HttpContext httpContext = new BasicHttpContext();
			httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			URI uri = new URIBuilder(getEndpoint()).
				setPath(path).
				setParameter("name", "test").
				build();

			HttpGet httpget = new HttpGet(uri);

			try (CloseableHttpResponse response = httpclient.execute(httpget, httpContext)) {
				HttpEntity entity = response.getEntity();

				assertEquals("test", read(entity));
			}

			for (int i = 0; i < 10; i++) {
				uri = new URIBuilder(getEndpoint()).
					setPath(path).
					build();

				httpget = new HttpGet(uri);

				try (CloseableHttpResponse response = httpclient.execute(httpget, httpContext)) {
					HttpEntity entity = response.getEntity();

					assertEquals("test", read(entity));
				}
			}

			uri = new URIBuilder(getEndpoint()).
				setPath(path).
				build();

			httpget = new HttpGet(uri);

			try (CloseableHttpResponse response = httpclient.execute(httpget)) {
				HttpEntity entity = response.getEntity();

				assertEquals("", read(entity));
			}
		}
		finally {
			tb5Bundle.uninstall();
		}
	}

	private String getEndpoint() {
		String[] endpoints = (String[])hsrReference.getProperty("osgi.http.endpoint");

		return endpoints[0];
	}

	@Override
	protected void setUp() throws Exception {
		hsrTracker = new ServiceTracker<>(bundleContext, HttpServiceRuntime.class, null);

		hsrTracker.open();

		hsr = hsrTracker.waitForService(timeout);

		hsrReference = hsrTracker.getServiceReference();

		hcbfTracker = new ServiceTracker<>(bundleContext, HttpClientBuilderFactory.class, null);

		hcbfTracker.open();

		hcbf = hcbfTracker.waitForService(timeout);

		assertNotNull(hsr);
	}

	@Override
	protected void tearDown() throws Exception {
		hsrTracker.close();
		hcbfTracker.close();
	}

	private static String read(HttpEntity entity) throws Exception {
		if (entity == null) {
			return null;
		}

		try (InputStream in = entity.getContent();
			java.util.Scanner s = new java.util.Scanner(in)) {

			s.useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
		}
	}

	private RequestInfoDTO waitFor(String path) throws InterruptedException {
		return waitFor(path, 10);
	}

	private RequestInfoDTO waitFor(String path, int intervals) throws InterruptedException {
		for (int j = intervals; j > 0; j--) {
			RequestInfoDTO requestInfoDTO = hsr.calculateRequestInfoDTO(path);

			if ((requestInfoDTO.servletDTO != null) || (requestInfoDTO.resourceDTO != null)) {
				return requestInfoDTO;
			}

			Thread.sleep(50);
		}

		assertTrue(String.format("%s not found in time", path), false);

		return null;
	}

	private HttpClientBuilderFactory hcbf;
	private HttpServiceRuntime hsr;
	private ServiceReference<HttpServiceRuntime> hsrReference;
	private ServiceTracker<HttpClientBuilderFactory, HttpClientBuilderFactory> hcbfTracker;
	private ServiceTracker<HttpServiceRuntime, HttpServiceRuntime> hsrTracker;

}
