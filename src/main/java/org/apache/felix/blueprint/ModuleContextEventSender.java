package org.apache.felix.blueprint;

import org.apache.felix.blueprint.context.ModuleContextImpl;
import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.context.ModuleContextEventConstants;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Apr 13, 2009
 * Time: 11:18:06 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ModuleContextEventSender extends ModuleContextEventConstants {

    void sendCreating(ModuleContext moduleContext);
    void sendCreated(ModuleContext moduleContext);
    void sendDestroying(ModuleContext moduleContext);
    void sendDestroyed(ModuleContext moduleContext);
    void sendWaiting(ModuleContext moduleContext, String[] serviceObjectClass, String serviceFilter);
    void sendFailure(ModuleContext moduleContext, Throwable cause);
    void sendFailure(ModuleContext moduleContext, Throwable cause, String[] serviceObjectClass, String serviceFilter);

    void destroy();

}
