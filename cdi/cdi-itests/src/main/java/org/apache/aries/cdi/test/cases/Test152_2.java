package org.apache.aries.cdi.test.cases;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;

public class Test152_2 extends AbstractTestCase {

	@Test
	public void checkUniqueComponentNames() throws Exception {
		Bundle tb152_2Bundle = installBundle("tb152_2.jar");

		try {
			getBeanManager(tb152_2Bundle);

			ContainerDTO containerDTO = getContainerDTO(cdiRuntime, tb152_2Bundle);
			assertThat(containerDTO).isNotNull();
			assertThat(containerDTO.errors).isNotNull().asList().isNotEmpty();
		}
		finally {
			tb152_2Bundle.uninstall();
		}
	}

	@Test
	public void checkUniqueComponentNames_b() throws Exception {
		Bundle tb152_2bBundle = installBundle("tb152_2b.jar");

		try {
			getBeanManager(tb152_2bBundle);

			ContainerDTO containerDTO = getContainerDTO(cdiRuntime, tb152_2bBundle);
			assertThat(containerDTO).isNotNull();
			assertThat(containerDTO.errors).isNotNull().asList().isNotEmpty();
		}
		finally {
			tb152_2bBundle.uninstall();
		}
	}

}
