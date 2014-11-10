package org.apache.aries.jpa.container.quiesce.impl;

import java.io.Closeable;

import org.apache.aries.jpa.container.impl.NLS;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuiesceParticipantFactory {
    /** The QuiesceParticipant implementation class name */
    private static final String QUIESCE_PARTICIPANT_CLASS = "org.apache.aries.quiesce.participant.QuiesceParticipant";

    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container");

    public static Closeable create(BundleContext context, QuiesceHandler quiesceHandler) {
        try {
            context.getBundle().loadClass(QUIESCE_PARTICIPANT_CLASS);
            // Class was loaded, register
            return new QuiesceParticipantImpl(context, quiesceHandler);
        } catch (ClassNotFoundException e) {
            _logger.info(NLS.MESSAGES.getMessage("quiesce.manager.not.there"));
            return null;
        }
    }

}
