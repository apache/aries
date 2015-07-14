package org.apache.aries.jpa.itest;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class AbstractJPAItest {
    private static Logger LOG = LoggerFactory.getLogger(AbstractJPAItest.class);
    protected static final String BLUE_CAR_PLATE = "A1AAA";
    protected static final String TEST_UNIT = "test-unit";
    protected static final String XA_TEST_UNIT = "xa-test-unit";
    protected static final String BP_TEST_UNIT = "bp-test-unit";
    protected static final String BP_XA_TEST_UNIT = "bp-xa-test-unit";
    protected static final String TEST_BUNDLE_NAME = "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle";

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected ConfigurationAdmin configAdmin;
    private Configuration config;

    /**
     * TODO check calls to this. Eventually switch to EmSupplier 
     */
    protected EntityManagerFactory getProxyEMF(String name) {
        return getEMF(name);
    }

    protected EntityManagerFactory getEMF(String name) {
        return getService(EntityManagerFactory.class, "osgi.unit.name=" + name);
    }

    public <T> T getService(Class<T> type, String filter) {
        return getService(type, filter, true);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> T getService(Class<T> type, String filter, boolean mandatory) {
        ServiceTracker tracker = null;
        try {
            String objClassFilter = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            String flt = filter != null ? "(&" + objClassFilter + sanitizeFilter(filter) + ")" : objClassFilter;
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open();

            Object svc = type.cast(tracker.waitForService(10000));
            if (svc == null && mandatory) {
                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            tracker.close();
        }
    }

    public String sanitizeFilter(String filter) {
        return filter.startsWith("(") ? filter : "(" + filter + ")";
    }

    /**
     * Helps to diagnose bundles that are not resolved as it will throw a detailed exception
     * 
     * @throws BundleException
     */
    public void resolveBundles() throws BundleException {
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.INSTALLED) {
                System.out.println("Found non resolved bundle " + bundle.getBundleId() + ":"
                    + bundle.getSymbolicName() + ":" + bundle.getVersion());
                bundle.start();
            }
        }
    }

    public Bundle getBundleByName(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    protected ServiceReference[] getEMFRefs(String name) throws InvalidSyntaxException {
        return bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(osgi.unit.name=" + name + ")");
    }

    private MavenArtifactProvisionOption mvnBundle(String groupId, String artifactId) {
        return mavenBundle(groupId, artifactId).versionAsInProject();
    }

    protected Option baseOptions() {
        String localRepo = System.getProperty("maven.repo.local");

        if (localRepo == null) {
            localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        }
        return composite(junitBundles(),
                         mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.7.2"),
                         mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.7.2"),
                         // this is how you set the default log level when using pax
                         // logging (logProfile)
                         systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                         when(localRepo != null).useOptions(vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo))
                         //,
            );
    }

    protected Option debug() {
        return vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
    }

    private Option ariesJpaInternal() {
        return composite(
                         frameworkProperty("org.osgi.framework.system.packages")
                         .value("javax.accessibility,javax.activation,javax.activity,javax.annotation,javax.annotation.processing,javax.crypto,javax.crypto.interfaces,javax.crypto.spec,javax.imageio,javax.imageio.event,javax.imageio.metadata,javax.imageio.plugins.bmp,javax.imageio.plugins.jpeg,javax.imageio.spi,javax.imageio.stream,javax.jws,javax.jws.soap,javax.lang.model,javax.lang.model.element,javax.lang.model.type,javax.lang.model.util,javax.management,javax.management.loading,javax.management.modelmbean,javax.management.monitor,javax.management.openmbean,javax.management.relation,javax.management.remote,javax.management.remote.rmi,javax.management.timer,javax.naming,javax.naming.directory,javax.naming.event,javax.naming.ldap,javax.naming.spi,javax.net,javax.net.ssl,javax.print,javax.print.attribute,javax.print.attribute.standard,javax.print.event,javax.rmi,javax.rmi.CORBA,javax.rmi.ssl,javax.script,javax.security.auth,javax.security.auth.callback,javax.security.auth.kerberos,javax.security.auth.login,javax.security.auth.spi,javax.security.auth.x500,javax.security.cert,javax.security.sasl,javax.sound.midi,javax.sound.midi.spi,javax.sound.sampled,javax.sound.sampled.spi,javax.sql,javax.sql.rowset,javax.sql.rowset.serial,javax.sql.rowset.spi,javax.swing,javax.swing.border,javax.swing.colorchooser,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.plaf.multi,javax.swing.plaf.synth,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.text.html.parser,javax.swing.text.rtf,javax.swing.tree,javax.swing.undo,javax.tools,javax.xml,javax.xml.bind,javax.xml.bind.annotation,javax.xml.bind.annotation.adapters,javax.xml.bind.attachment,javax.xml.bind.helpers,javax.xml.bind.util,javax.xml.crypto,javax.xml.crypto.dom,javax.xml.crypto.dsig,javax.xml.crypto.dsig.dom,javax.xml.crypto.dsig.keyinfo,javax.xml.crypto.dsig.spec,javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,javax.xml.soap,"
                             + "javax.xml.stream; version=1.0,javax.xml.stream.events; version=1.0,javax.xml.stream.util; version=1.0,"
                             + "javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stax,javax.xml.transform.stream,javax.xml.validation,javax.xml.ws,javax.xml.ws.handler,javax.xml.ws.handler.soap,javax.xml.ws.http,javax.xml.ws.soap,javax.xml.ws.spi,javax.xml.xpath,org.ietf.jgss,org.omg.CORBA,org.omg.CORBA.DynAnyPackage,org.omg.CORBA.ORBPackage,org.omg.CORBA.TypeCodePackage,org.omg.CORBA.portable,org.omg.CORBA_2_3,org.omg.CORBA_2_3.portable,org.omg.CosNaming,org.omg.CosNaming.NamingContextExtPackage,org.omg.CosNaming.NamingContextPackage,org.omg.Dynamic,org.omg.DynamicAny,org.omg.DynamicAny.DynAnyFactoryPackage,org.omg.DynamicAny.DynAnyPackage,org.omg.IOP,org.omg.IOP.CodecFactoryPackage,org.omg.IOP.CodecPackage,org.omg.Messaging,org.omg.PortableInterceptor,org.omg.PortableInterceptor.ORBInitInfoPackage,org.omg.PortableServer,org.omg.PortableServer.CurrentPackage,org.omg.PortableServer.POAManagerPackage,org.omg.PortableServer.POAPackage,org.omg.PortableServer.ServantLocatorPackage,org.omg.PortableServer.portable,org.omg.SendingContext,org.omg.stub.java.rmi,org.w3c.dom,org.w3c.dom.bootstrap,org.w3c.dom.css,org.w3c.dom.events,org.w3c.dom.html,org.w3c.dom.ls,org.w3c.dom.ranges,org.w3c.dom.stylesheets,org.w3c.dom.traversal,org.w3c.dom.views,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers"),

                             mvnBundle("org.ow2.asm", "asm-all"),
                             mvnBundle("org.apache.felix", "org.apache.felix.configadmin"),
                             mvnBundle("org.apache.felix", "org.apache.felix.coordinator"),

                             mvnBundle("org.apache.aries.proxy", "org.apache.aries.proxy.api"),
                             mvnBundle("org.apache.aries.proxy", "org.apache.aries.proxy.impl"),
                             mvnBundle("org.apache.aries", "org.apache.aries.util"),

                             mvnBundle("org.apache.aries.jndi", "org.apache.aries.jndi.api"),
                             mvnBundle("org.apache.aries.jndi", "org.apache.aries.jndi.core"),
                             mvnBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url"),

                             mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api"),
                             mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core"),

                             mvnBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api"),
                             mvnBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container"),
                             mvnBundle("org.apache.aries.jpa", "org.apache.aries.jpa.support"),
                             mvnBundle("org.apache.aries.jpa", "org.apache.aries.jpa.blueprint"),

                             mvnBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
                             mvnBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager"),
                             mvnBundle("org.apache.aries.transaction", "org.apache.aries.transaction.blueprint"),

                             mvnBundle("org.apache.derby", "derby")
            );
    }

    protected Option ariesJpa20() {
        return composite(
                         ariesJpaInternal(),
                         mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec", "1.1")
            );
    }

    protected Option ariesJpa21() {
        return composite(
                         ariesJpaInternal(),
                         mvnBundle("org.eclipse.persistence", "javax.persistence")
            );
    }

    protected Option eclipseLink() {
        return composite(
                         mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.commons-dbcp"),
                         mvnBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa"),
                         mvnBundle("org.eclipse.persistence", "org.eclipse.persistence.core"),
                         mvnBundle("org.eclipse.persistence", "org.eclipse.persistence.asm"),
                         mvnBundle("org.eclipse.persistence", "org.eclipse.persistence.antlr"),
                         mvnBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa.jpql"),
                         mvnBundle("org.apache.aries.jpa", "org.apache.aries.jpa.eclipselink.adapter")
            );
    }

    protected Option openJpa() {
        return composite(
                         mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.cglib"), //
                         mvnBundle("commons-pool", "commons-pool"), //
                         mvnBundle("commons-lang", "commons-lang"), //
                         mvnBundle("commons-collections", "commons-collections"), //
                         mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
                         mvnBundle("org.apache.geronimo.specs", "geronimo-servlet_2.5_spec"),
                         mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.commons-dbcp"),
                         mvnBundle("org.apache.xbean", "xbean-asm4-shaded"),
                         mvnBundle("org.apache.openjpa", "openjpa")
            );
    }

    protected Option hibernate() {
        return composite(
                         mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.antlr"),
                         mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.ant"),
                         mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.dom4j"),
                         mvnBundle("org.apache.servicemix.bundles" , "org.apache.servicemix.bundles.serp"),
                         mvnBundle("com.fasterxml", "classmate"),
                         mvnBundle("org.javassist", "javassist"),
                         mvnBundle("org.jboss.logging", "jboss-logging"),
                         mvnBundle("org.hibernate.common", "hibernate-commons-annotations"), 
                         mvnBundle("org.jboss", "jandex"),
                         mvnBundle("org.hibernate", "hibernate-core"),
                         mvnBundle("org.hibernate", "hibernate-entitymanager"),
                         mvnBundle("org.hibernate", "hibernate-osgi")
            );
    }

    protected Option derbyDSF() {
        return composite(
                         mvnBundle("org.ops4j.pax.jdbc", "pax-jdbc-spec"), //
                         mvnBundle("org.ops4j.pax.jdbc", "pax-jdbc-derby"), //
                         mvnBundle("org.apache.commons", "commons-pool2"), //
                         mvnBundle("commons-logging", "commons-logging"), //
                         mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.cglib"), //
                         mvnBundle("org.apache.commons", "commons-dbcp2"), //
                         mvnBundle("org.ops4j.pax.jdbc", "pax-jdbc-pool-common"), //
                         mvnBundle("org.ops4j.pax.jdbc", "pax-jdbc-pool-dbcp2"), //
                         mvnBundle("org.ops4j.pax.jdbc", "pax-jdbc-config")
            );
    }

    protected Option testBundle() {
        return mvnBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle");
    }

    protected Option testBundleBlueprint() {
        return composite(
                         testBundle(),
                         mvnBundle("org.apache.aries.jpa.itest", "org.apache.aries.jpa.container.itest.bundle.blueprint")
            );

    }

    protected MavenArtifactProvisionOption testBundleEclipseLink() {
        return mvnBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle.eclipselink");
    }

    @Before
    public void createConfigForDataSource() throws IOException {
        if (config == null) {
            config = configAdmin.createFactoryConfiguration("org.ops4j.datasource", null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, "org.apache.derby.jdbc.EmbeddedDriver-pool-xa");
            props.put(DataSourceFactory.JDBC_URL, "jdbc:derby:memory:TEST1;create=true");
            props.put("dataSourceName", "testds");
            config.update(props);
            LOG.info("Created DataSource config testds");
        }
    }

    protected Car createBlueCar() {
        Car car = new Car();
        car.setNumberOfSeats(5);
        car.setEngineSize(1200);
        car.setColour("blue");
        car.setNumberPlate(BLUE_CAR_PLATE);
        return car;
    }

    protected Car createGreenCar() {
        Car car;
        car = new Car();
        car.setNumberOfSeats(7);
        car.setEngineSize(1800);
        car.setColour("green");
        car.setNumberPlate("B2BBB");
        return car;
    }

    protected void assertBlueCar(Car car) {
        assertEquals(5, car.getNumberOfSeats());
        assertEquals(1200, car.getEngineSize());
        assertEquals("blue", car.getColour());
        assertEquals(BLUE_CAR_PLATE, car.getNumberPlate());
    }

    protected void assertGreenCar(Car car) {
        assertEquals(7, car.getNumberOfSeats());
        assertEquals(1800, car.getEngineSize());
        assertEquals("green", car.getColour());
        assertEquals("B2BBB", car.getNumberPlate());
    }
}
