package org.apache.aries.subsystem.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.FutureTask;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemEvent;
import org.apache.aries.subsystem.spi.Resource;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemFutureTask extends FutureTask<Subsystem> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SubsystemFutureTask.class);
    
    public SubsystemFutureTask(Runnable runnable, final BundleContext context, final String url, final InputStream is) {
        super(runnable, null);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("FutureTask created to install subsystem url {}.", url);
        }
    }


    /**
     * Set the result of the future's work. This is used by SubsystemAdmin to
     * set the result on the future that it initiated.
     */
    public void set(Subsystem result) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Subsystem FutureTask installation completed.");
        }
        super.set(result);
    }

}
