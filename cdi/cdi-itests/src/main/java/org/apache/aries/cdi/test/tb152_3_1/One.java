package org.apache.aries.cdi.test.tb152_3_1;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.EventMetadata;
import javax.inject.Inject;

import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.propertytypes.ServiceDescription;

@ApplicationScoped
@Bean
@SuppressWarnings({"rawtypes", "unchecked"})
public class One {

	@Inject
	@Reference
	@ServiceDescription("onInitialized")
	Function onInitialized;

	@Inject
	@Reference
	@ServiceDescription("onBeforeDestroyed")
	Function onBeforeDestroyed;

	@Inject
	@Reference
	@ServiceDescription("onDestroyed")
	Function onDestroyed;

	void onInitialized(@Observes @Initialized(ComponentScoped.class) Object obj, EventMetadata metadata, BeanManager bm) {
		onInitialized.apply(new Object[] {obj, bm, metadata});
	}

	void onBeforeDestroyed(@Observes @BeforeDestroyed(ComponentScoped.class) Object obj, EventMetadata metadata, BeanManager bm) {
		onBeforeDestroyed.apply(new Object[] {obj, bm, metadata});
	}

	void onDestroyed(@Observes @Destroyed(ComponentScoped.class) Object obj, EventMetadata metadata, BeanManager bm) {
		onDestroyed.apply(new Object[] {obj, bm, metadata});
	}

}
