/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jmx.core.whiteboard;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.framework.BundleState;
import org.apache.aries.jmx.framework.Framework;
import org.apache.aries.jmx.framework.PackageState;
import org.apache.aries.jmx.framework.ServiceState;
import org.apache.aries.jmx.framework.StateConfig;
import org.apache.aries.jmx.util.ObjectNameUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.FrameworkMBean;
import org.osgi.jmx.framework.PackageStateMBean;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.osgi.jmx.service.cm.ConfigurationAdminMBean;
import org.osgi.jmx.service.permissionadmin.PermissionAdminMBean;
import org.osgi.jmx.service.provisioning.ProvisioningServiceMBean;
import org.osgi.jmx.service.useradmin.UserAdminMBean;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer
{
  private ServiceTracker tracker;
  private BundleContext ctx;
  private ConcurrentMap<Long, ServiceRegistration> _provisioningMBeans = new ConcurrentHashMap<Long, ServiceRegistration>();
  private ConcurrentMap<Long, ServiceRegistration> _userAdminMBeans = new ConcurrentHashMap<Long, ServiceRegistration>();
  private ConcurrentMap<Long, ServiceRegistration> _configAdminMBeans = new ConcurrentHashMap<Long, ServiceRegistration>();

  private AtomicReference<ServiceRegistration> _serviceStateMbean = new AtomicReference<ServiceRegistration>();
  private AtomicReference<ServiceRegistration> _permissionAdminMbean = new AtomicReference<ServiceRegistration>();
  private AtomicReference<ServiceRegistration> _packageStateMbean = new AtomicReference<ServiceRegistration>();
  private AtomicReference<ServiceRegistration> _bundleState = new AtomicReference<ServiceRegistration>();
  private AtomicReference<ServiceRegistration> _framework = new AtomicReference<ServiceRegistration>();

  private AtomicReference<ServiceReference> _startLevel = new AtomicReference<ServiceReference>();
  private AtomicReference<ServiceReference> _packageAdmin = new AtomicReference<ServiceReference>();

  private static final String PACKAGE_ADMIN = "org.osgi.service.packageadmin.PackageAdmin";
  private static final String START_LEVEL = "org.osgi.service.startlevel.StartLevel";
  private static final String PERMISSION_ADMIN = "org.osgi.service.permissionadmin.PermissionAdmin";
  private static final String CONFIG_ADMIN = "org.osgi.service.cm.ConfigurationAdmin";
  private static final String USER_ADMIN = "org.osgi.service.useradmin.UserAdmin";
  private static final String PROVISIONING_SERVICE = "org.osgi.service.provisioning.ProvisioningService";

  private Logger logger;
  private StateConfig stateConfig;

  private class MBeanServiceProxy<T> implements ServiceFactory
  {
    private Factory<T> objectFactory;
    private AtomicReference<T> result = new AtomicReference<T>();

    private MBeanServiceProxy(Factory<T> factory) {
      objectFactory = factory;
    }

    public Object getService(Bundle bundle, ServiceRegistration registration)
    {
      if (result.get() == null) {
        result.compareAndSet(null, objectFactory.create());
      }
      return result.get();
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
    {
    }
  }

  private interface Factory<T>
  {
    public abstract T create();
  }

  private abstract class BaseFactory<T> implements Factory<T>
  {
    public abstract T create(PackageAdmin pa, StartLevel sl);
    public final T create()
    {
      StartLevel sl = null;
      PackageAdmin pa = null;

      ServiceReference slRef = _startLevel.get();
      if (slRef != null) {
        sl = (StartLevel) ctx.getService(slRef);
      }
      ServiceReference paRef = _packageAdmin.get();

      if (paRef != null) {
        pa = (PackageAdmin) ctx.getService(paRef);
      }

      if (pa == null) {
        ctx.ungetService(slRef);
      }

      if (sl != null && pa != null) {
        return create(pa, sl);
      } else {
        return null;
      }
    }
  }

  public void start(BundleContext context) throws Exception
  {
    ctx = context;
    logger = new Logger(ctx);

    Filter filter = getFilter(context, PACKAGE_ADMIN, START_LEVEL,
        PERMISSION_ADMIN, CONFIG_ADMIN, USER_ADMIN,
        PROVISIONING_SERVICE);

    tracker = new ServiceTracker(context, filter, this);
    tracker.open();

    stateConfig = StateConfig.register(context);

    registerMBean(ServiceStateMBean.class.getName(), new Factory<ServiceStateMBean>() {
      public ServiceStateMBean create()
      {
        return new ServiceState(ctx, stateConfig, logger);
      }
    }, ServiceStateMBean.OBJECTNAME, _serviceStateMbean );
  }

  private Filter getFilter(BundleContext ctx, String ... services) throws InvalidSyntaxException
  {
    StringBuilder builder = new StringBuilder("(|");

    for (String type : services) {
      builder.append('(');
      builder.append(Constants.OBJECTCLASS);
      builder.append('=');
      builder.append(type);
      builder.append(')');
    }

    builder.append(')');
    return ctx.createFilter(builder.toString());
  }

  public void stop(BundleContext context) throws Exception
  {
    stateConfig = null;
    tracker.close();
  }

  public Object addingService(ServiceReference reference)
  {
    Object tracked = null;

    String[] types = (String[]) reference.getProperty(Constants.OBJECTCLASS);

    for (String t : types) {
      if (PACKAGE_ADMIN.equals(t)) {
        foundPackageAdmin(reference);
        tracked = reference;
      } else if (START_LEVEL.equals(t)) {
        foundStartLevel(reference);
        tracked = reference;
      } else if (PERMISSION_ADMIN.equals(t)) {
        foundPermissionAdmin(reference);
        tracked = reference;
      } else if (CONFIG_ADMIN.equals(t)) {
        foundConfigAdmin(reference);
        tracked = reference;
      } else if (USER_ADMIN.equals(t)) {
        foundUserAdmin(reference);
        tracked = reference;
      } else if (PROVISIONING_SERVICE.equals(t)) {
        foundProvisioningService(reference);
        tracked = reference;
      }
    }

    return tracked;
  }

  private <T> void registerMBean(String type, Factory<T> factory, String objectName, AtomicReference<ServiceRegistration> result)
  {
    synchronized (result) {
      ServiceRegistration reg = registerAnMbean(type, factory, objectName);

      if (!!!result.compareAndSet(null, reg)) {
        reg.unregister();
      }
    }
  }

  private <T> void registerMBean(String type, Factory<T> factory, String objectName, ConcurrentMap<Long, ServiceRegistration> mbeans,
      ServiceReference referencedServices, String underlyingType)
  {
    try {
      Class.forName(underlyingType);
      if (referencedServices.isAssignableTo(ctx.getBundle(), underlyingType)) {
        ServiceRegistration reg = registerAnMbean(type, factory, objectName);

        Long id = (Long) reg.getReference().getProperty(Constants.SERVICE_ID);
        mbeans.put(id, reg);
      }
    } catch (ClassNotFoundException e) {
    }
  }

  private <T> ServiceRegistration registerAnMbean(String type, Factory<T> factory, String objectName)
  {
    Hashtable<String, Object> properties = new Hashtable<String, Object>();
    properties.put("jmx.objectname", ObjectNameUtils.createFullObjectName(ctx, objectName));

    Object service = new MBeanServiceProxy<T>(factory);

    ServiceRegistration reg = ctx.registerService(type, service, properties);
    return reg;
  }

  private void foundPermissionAdmin(final ServiceReference reference)
  {
    registerMBean(PermissionAdminMBean.class.getName(), new Factory<PermissionAdminMBean>() {
      public PermissionAdminMBean create()
      {
        PermissionAdmin service = (PermissionAdmin) ctx.getService(reference);

        if (service == null) return null;
        else return new org.apache.aries.jmx.permissionadmin.PermissionAdmin(service);
      }
    }, PermissionAdminMBean.OBJECTNAME, _permissionAdminMbean);
  }

  private void foundProvisioningService(final ServiceReference reference)
  {
    registerMBean(ProvisioningServiceMBean.class.getName(), new Factory<ProvisioningServiceMBean>() {
        public ProvisioningServiceMBean create()
        {
          ProvisioningService service = (ProvisioningService) ctx.getService(reference);

          if (service == null) return null;
          else return new org.apache.aries.jmx.provisioning.ProvisioningService(service);
        }
      }, ProvisioningServiceMBean.OBJECTNAME, _provisioningMBeans, reference, PROVISIONING_SERVICE);
  }

  private void foundUserAdmin(final ServiceReference reference)
  {
    try {
      Class.forName(USER_ADMIN);
      if (reference.isAssignableTo(ctx.getBundle(), USER_ADMIN)) {
        registerMBean(UserAdminMBean.class.getName(), new Factory<UserAdminMBean>() {
          public UserAdminMBean create()
          {
            UserAdmin service = (UserAdmin) ctx.getService(reference);

            if (service == null) return null;
            else return new org.apache.aries.jmx.useradmin.UserAdmin(service);
          }
        }, UserAdminMBean.OBJECTNAME, _userAdminMBeans, reference, USER_ADMIN);
      }
    } catch (ClassNotFoundException e) {
    }
  }

  private void foundConfigAdmin(final ServiceReference reference)
  {
    registerMBean(ConfigurationAdminMBean.class.getName(), new Factory<ConfigurationAdminMBean>() {
      public ConfigurationAdminMBean create()
      {
        ConfigurationAdmin service = (ConfigurationAdmin) ctx.getService(reference);

        if (service == null) return null;
        else return new org.apache.aries.jmx.cm.ConfigurationAdmin(service);
      }
    }, ConfigurationAdminMBean.OBJECTNAME, _configAdminMBeans, reference, CONFIG_ADMIN);
  }

  private void foundStartLevel(final ServiceReference reference)
  {
    if (_startLevel.compareAndSet(null, reference)) {
      registerBundleStateAndFrameworkIfPossible();
    }
  }

  private void foundPackageAdmin(final ServiceReference reference)
  {
    registerMBean(PackageStateMBean.class.getName(), new Factory<PackageStateMBean>() {
      public PackageStateMBean create()
      {
        PackageAdmin service = (PackageAdmin) ctx.getService(reference);

        if (service == null) return null;
        else return new PackageState(ctx, service);
      }
    }, PackageStateMBean.OBJECTNAME, _packageStateMbean);

    if (_packageAdmin.compareAndSet(null, reference)) {
      registerBundleStateAndFrameworkIfPossible();
    }
  }

  // This method is synchronized to ensure that notification of StartLevel and PackageAdmin
  // on different threads at the same time doesn't cause problems. It only affects these services
  // so it shouldn't be too expensive.
  private synchronized void registerBundleStateAndFrameworkIfPossible()
  {
      if (_bundleState.get() == null && _startLevel.get() != null && _packageAdmin.get() != null) {
        registerMBean(BundleStateMBean.class.getName(), new BaseFactory<BundleStateMBean>() {
          @Override
          public BundleStateMBean create(PackageAdmin pa, StartLevel sl)
          {
            return new BundleState(ctx, pa, sl, stateConfig, logger);
          }
        }, BundleStateMBean.OBJECTNAME, _bundleState);
      }
      if (_framework.get() == null && _startLevel.get() != null && _packageAdmin.get() != null) {
        registerMBean(FrameworkMBean.class.getName(), new BaseFactory<FrameworkMBean>() {
          @Override
          public FrameworkMBean create(PackageAdmin pa, StartLevel sl)
          {
            return new Framework(ctx, sl, pa);
          }
        }, FrameworkMBean.OBJECTNAME, _framework);
      }
  }

  public void modifiedService(ServiceReference reference, Object service)
  {
  }

  public void removedService(ServiceReference reference, Object service)
  {
    String[] types = (String[]) reference.getProperty(Constants.OBJECTCLASS);

    for (String t : types) {
      if (PACKAGE_ADMIN.equals(t)) {
        lostPackageAdmin(reference);
      } else if (START_LEVEL.equals(t)) {
        lostStartLevel(reference);
      } else if (PERMISSION_ADMIN.equals(t)) {
        lostPermissionAdmin(reference);
      } else if (CONFIG_ADMIN.equals(t)) {
        lostConfigAdmin(reference);
      } else if (USER_ADMIN.equals(t)) {
        lostUserAdmin(reference);
      } else if (PROVISIONING_SERVICE.equals(t)) {
        lostProvisioningService(reference);
      }
    }

  }

  private void lostProvisioningService(ServiceReference reference)
  {
    unregister(reference, _provisioningMBeans);
  }

  private void lostUserAdmin(ServiceReference reference)
  {
    unregister(reference, _userAdminMBeans);
  }

  private void lostConfigAdmin(ServiceReference reference)
  {
    unregister(reference, _configAdminMBeans);
  }

  private void unregister(ServiceReference reference, ConcurrentMap<Long, ServiceRegistration> mbeans)
  {
    Long id = (Long) reference.getProperty(Constants.SERVICE_ID);
    ServiceRegistration reg = mbeans.remove(id);
    if (reg != null) reg.unregister();
  }

  private void lostPermissionAdmin(ServiceReference reference)
  {
    safeUnregister(_permissionAdminMbean);
  }

  private void lostStartLevel(ServiceReference reference)
  {
    if (_startLevel.compareAndSet(reference, null)) {
      safeUnregister(_bundleState);
      safeUnregister(_framework);
    }
  }

  private void lostPackageAdmin(ServiceReference reference)
  {
    if (_packageAdmin.compareAndSet(reference, null)) {
      safeUnregister(_bundleState);
      safeUnregister(_framework);

      safeUnregister(_packageStateMbean);
    }
  }

  private void safeUnregister(AtomicReference<ServiceRegistration> atomicRegistration)
  {
    ServiceRegistration reg = atomicRegistration.getAndSet(null);
    if (reg != null) reg.unregister();
  }
}