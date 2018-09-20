package org.apache.aries.cdi.test.tb152_2;

import javax.inject.Named;

import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.SingleComponent;

@Bean
@Named("same")
@SingleComponent
public class One {
}
