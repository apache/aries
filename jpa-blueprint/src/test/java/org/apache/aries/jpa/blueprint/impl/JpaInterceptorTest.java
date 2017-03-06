package org.apache.aries.jpa.blueprint.impl;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

public class JpaInterceptorTest implements Runnable {
    private JpaInterceptor interceptor;
    private AtomicBoolean result = new AtomicBoolean(true);

    @Test
    public void testThreadSafePreCall() throws InterruptedException {
        BlueprintContainer container = new BlueprintContainerStub();
        interceptor = new JpaInterceptor(container, "coordinator", "em");

        Thread t1 = new Thread(this);
        Thread t2 = new Thread(this);

        t1.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t2.start();
        synchronized (result) {
            result.wait();
        }
        Assert.assertTrue(result.get());
    }

    @Override
    public void run() {
        ComponentMetadata cm = new ComponentMetadata() {

            @Override
            public String getId() {
                return "testMetadata";
            }

            @Override
            public List<String> getDependsOn() {
                return null;
            }

            @Override
            public int getActivation() {
                return 0;
            }
        };
        Method m;
        try {
            m = this.getClass().getMethod("run", null);
            interceptor.preCall(cm, m, null);
        } catch (Throwable e) {
            synchronized (result) {
                result.set(false);
            }
        } finally {
            synchronized (result) {
                result.notifyAll();
            }
        }
    }
}
