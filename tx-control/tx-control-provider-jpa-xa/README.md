Sample OSGi Transaction Control JPA Provider (XA)
-------------------------------------------------

This module is a prototype implementation of the OSGi Transaction Control JPA resource provider. It supports JPA 2.1 using XA transactions, and is tested against Hibernate 5.0.9, EclipseLink 2.6.0 and OpenJPA 2.4.1.

The Transaction Control Service (RFC-221) is an in-progress RFC publicly available from the OSGi Alliance: https://github.com/osgi/design/blob/master/rfcs/rfc0221/rfc-0221-TransactionControl.pdf

Given that the RFC is non-final the OSGi API declared in this project is subject to change at any time up to its official release. Also the behaviour of this implementation may not always be up-to-date with the latest wording in the RFC. The project maintainers will, however try to keep pace with the RFC, and to ensure that the implementations are compliant with any OSGi specifications that result from the RFC.

# When should I use this module?

If two-phase commit is needed across multiple resources then it is best to pair this module with the tx-control-service-xa bundle.

If you wish to use entirely lightweight, resource-local transactions then then the tx-control-service-local and tx-control-provider-jpa-local bundles should be used instead of this bundle

# Using the JPA Provider bundle

This Resource Provider is used in conjunction with a TransactionControl service to provide scoped access to a JPA EntityManager.

## Prerequisites

In order to use scoped JPA access the runtime must contain a JPA provider (for example Hibernate), an implementation of the OSGi JPA service (e.g. Aries JPA), and a persistence bundle.

### Suitable Persistence bundles

OSGi Persistence bundles contain the persistence descriptor (typically an XML file called META-INF/persistence.xml), all of the JPA Entities, and a Meta-Persistence header pointing at the persistence descriptor. (See the JPA service specification for more details).

Unlike "normal" JPA it is usually best not to fully declare the persistence unit in the persistence descriptor. In particular it is a good idea to avoid putting any database configuration in the persistence unit. By not configuring the database inside the bundle the persistence unit remains decoupled, and can be reconfigured for any database at runtime.

For example the following persistence unit:

    <persistence-unit name="test-unit"/>

can be reconfigured to use any database and create/drop tables as appropriate. Configuration for the persistence unit can be provided using Configuration Admin and the EntityManagerFactory Builder.


## Creating a resource programmatically

Preparing a resource for use is very simple. Create a JPAEntityManagerProvider using the the JPAEntityManagerProviderFactory, then connect the provider to a TransactionControl service. This will return a thread-safe JPA EntityManager that can then be used in any ongoing scoped work.

The normal inputs to a JPAEntityManagerProviderFactory are an EntityManagerFactoryBuilder, some JPA properties to connect to the database with, and some properties to control the resource provider.

    @Reference
    TransactionControl txControl;

    @Reference
    DataSourceFactory dsf;

    @Reference
    EntityManagerFactoryBuilder emfb;

    @Reference
    JPAEntityManagerProviderFactory providerFactory;

    EntityManager em;

    @Activate
    void start(Config config) {

        Properties jdbcProps = new Properties();
        jdbcProps.put(JDBC_URL, config.url());
        jdbcProps.put(JDBC_USER, config.user());
        jdbcProps.put(JDBC_PASSWORD, config._password());

        Map<String, Object> jpaProps = new HashMap<>();
        jpaProps.put("javax.persistence.nonJtaDataSource", 
                    dsf.createDataSource(jdbcProps));

        em = providerFactory.getProviderFor(emfb, jpaProps,
                    null).getResource(txControl);
    }

    public void findUserName(String id) {
        txControl.required(() -> {
                // Use the EntityManager in here
            });
    } 

If the JPA EntityManagerFactory is already configured then it can be passed into the JPAEntityManagerProviderFactory instead of an EntityManagerFactoryBuilder and JPA configuration.


## Creating a resource using a factory configuration

Whilst it is simple to use a EntityManagerFactoryBuilder it does require some lifecycle code to be written. It is therefore possible to directly create JPA resources using factory configurations. When created, the factory service will listen for an applicable EntityManagerFactoryBuilder and potentially also a DataSourceFactory. Once suitable services are available then a JPAEntityManagerProvider service will be published. 

Configuration properties (except the JPA/JDBC password) are set as service properties for the registered JPAEntityManagerProvider. These properties may therefore be used in filters to select a particular provider.

    @Reference
    TransactionControl txControl;

    @Reference(target="(osgi.unit.name=test-unit)")
    JPAEntityManagerProvider provider;

    EntityManager em;

    @Activate
    void start(Config config) {
        em = provider.getResource(txControl);
    }

    public void findUserName(String id) {
        txControl.required(() -> {
                // Use the connection in here
            });
    } 



The factory pid is _org.apache.aries.tx.control.jpa.local_ and it may use the following properties (all optional aside from *osgi.unit.name*):

### Resource Provider properties

* *osgi.unit.name* : The name of the persistence unit that this configuration relates to.

* *aries.emf.builder.target.filter* : The target filter to use when searching for an EntityManagerFactoryBuilder. If not specified then any builder for the named persistence unit will be used.

* *aries.jpa.property.names* : The names of the properties to pass to the EntityManagerFactoryBuilder when creating the EntityManagerFactory. By default all properties are copied.

* *aries.dsf.target.filter* : The target filter to use when searching for a DataSourceFactory. If not specified then *osgi.jdbc.driver.class* must be specified.

* *aries.jdbc.property.names* : The names of the properties to pass to the DataSourceFactory when creating the JDBC resources.

* *osgi.jdbc.driver.class* : Used to locate the DataSourceFactory service if the *aries.dsf.target.filter* is not set.

* *osgi.local.enabled* : Defaults to false. If true then resource creation will fail

* *osgi.xa.enabled* : Defaults to true. If false then resource creation will fail

* *osgi.connection.pooling.enabled* : Defaults to true. If true then the Database connections will be pooled.

* *osgi.connection.max* : Defaults to 10. The maximum number of connections that should be kept in the pool

* *osgi.connection.min* : Defaults to 10. The minimum number of connections that should be kept in the pool

* *osgi.connection.timeout* : Defaults to 30,000 (30 seconds). The maximum time in milliseconds to block when waiting for a database connection

* *osgi.idle.timeout* : Defaults to 180,000 (3 minutes). The time in milliseconds before an idle connection is eligible to be closed.

* *osgi.connection.timeout* : Defaults to 10,800,000 (3 hours). The maximum time in milliseconds that a connection may remain open before being closed.


### JDBC properties

The following properties will automatically be passed to the DataSourceFactory if they are present. The list of properties may be overridden using the *aries.jdbc.property.names* property if necessary.

* *databaseName* : The name of the database

* *dataSourceName* : The name of the dataSource that will be created

* *description* : A description of the dataSource being created

* *networkProtocol* : The network protocol to use.

* *portNumber* : The port number to use

* *roleName* : The name of the JDBC role

* *serverName* : The name of the database server

* *url* : The JDBC url to use (often used instead of other properties such as *serverName*, *portNumber* and *databaseName*).

* *user* : The JDBC user

* *password* : The JDBC password


### JPA properties

The following properties are potentially useful when configuring JPA:

*javax.persistence.schema-generation.database.action* : May be used to automatically create the database tables (see the OSGi spec)

* Other provider specific properties, for example configuring second-level caching.

