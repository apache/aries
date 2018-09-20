package org.apache.aries.cdi.test.tb152_2b;

import javax.inject.Named;

import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.SingleComponent;

@Bean
@Named("one")
@SingleComponent
public class Two {
}
