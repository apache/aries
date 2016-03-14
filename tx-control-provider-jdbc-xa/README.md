Sample OSGi Transaction Control JDBC Provider (XA)
--------------------------------------------------

This module is a prototype implementation of the OSGi Transaction Control JDBC resource provider. It supports XA and Local transactions.

The Transaction Control Service (RFC-221) is an in-progress RFC publicly available from the OSGi Alliance: https://github.com/osgi/design/blob/master/rfcs/rfc0221/rfc-0221-TransactionControl.pdf

Given that the RFC is non-final the OSGi API declared in this project is subject to change at any time up to its official release. Also the behaviour of this implementation may not always be up-to-date with the latest wording in the RFC. The project maintainers will, however try to keep pace with the RFC, and to ensure that the implementations are compliant with any OSGi specifications that result from the RFC.

# When should I use this module?

If two-phase commit is needed across multiple resources then it is best to pair this module with the tx-control-service-xa bundle.

If you wish to use entirely lightweight, resource-local transactions then then the tx-control-service-local and tx-control-provider-jdbc-local bundles should be used instead of this bundle

# Using the JDBC Provider bundle

This Resource Provider is used in conjunction with a TransactionControl service to provide scoped access to JDBC.

## Creating a resource programmatically

Preparing a resource for use is very simple. Create a JDBCConnectionProvider using the the JDBCConnectionProviderFactory, then connect the provider to a TransactionControl service. This will return a thread-safe JDBC connection that can then be used in any ongoing scoped work.

The normal inputs to a JDBCConnectionProviderFactory are a DataSourceFactory, some jdbc properties to connect to the database with, and some properties to control the resource provider (such as connection pooling)


    @Reference
    TransactionControl txControl;

    @Reference
    DataSourceFactory dsf;

    @Reference
    JDBCConnectionProviderFactory providerFactory;

    Connection conn;

    @Activate
    void start(Config config) {

        Properties jdbcProps = new Properties();
        jdbcProps.put(JDBC_URL, config.url());
        jdbcProps.put(JDBC_USER, config.user());
        jdbcProps.put(JDBC_PASSWORD, config._password());

        Map<String, Object> providerProps = new HashMap<>();
        providerProps.put(MAX_POOL_SIZE, 8);

        conn = providerFactory.getProviderFor(dsf, 
                   jdbcProps, providerProps).getResource(txControl);
    }

    public void findUserName(String id) {
        txControl.required(() -> {
                // Use the connection in here
            });
    } 

If the JDBC DataSource/Driver is already configured then it can be passed in to the JDBCConnectionProviderFactory instead of a DataSourceFactory and JDBC configuration.


## Creating a resource using a factory configuration

Whilst it is simple to use a JDBCConnectionProviderFactory it does require some lifecycle code to be written. It is therefore possible to directly create JDBC resources using factory configurations. When created, the factory service will listen for an applicable DataSourceFactory. Once a suitable DataSourceFactory is available then a JDBCConnectionProvider service will be published. 

Configuration properties (except the JDBC password) are set as service properties for the registered JDBCConnectionProvider. These properties may therefore be used in filters to select a particular provider.

    @Reference
    TransactionControl txControl;

    @Reference(target="(dataSourceName=myDataSource)")
    JDBCConnectionProvider provider;

    Connection conn;

    @Activate
    void start(Config config) {
        conn = provider.getResource(txControl);
    }

    public void findUserName(String id) {
        txControl.required(() -> {
                // Use the connection in here
            });
    } 



The factory pid is _org.apache.aries.tx.control.jdbc.xa_ and it may use the following properties (all optional):

### Resource Provider properties

* *aries.dsf.target.filter* : The target filter to use when searching for a DataSourceFactory. If not specified then *osgi.jdbc.driver.class* must be specified.

* *aries.jdbc.property.names* : The names of the properties to pass to the DataSourceFactory when creating the JDBC resources

* *osgi.jdbc.driver.class* : Used to locate the DataSourceFactory service if the *aries.dsf.target.filter* is not set.

* *osgi.local.enabled* : Defaults to true. If false then this resource will not participate in local transactions, and will fail if used within one. One of *osgi.local.enabled* and *osgi.xa.enabled* must be true.

* *osgi.xa.enabled* : Defaults to true. If false then this resource will not participate in xa transactions, and will fail if used within one. One of *osgi.local.enabled* and *osgi.xa.enabled* must be true.

* *osgi.connection.pooling.enabled* : Defaults to true. If true then the Database connections will be pooled.

* *osgi.connection.max* : Defaults to 10. The maximum number of connections that should be kept in the pool

* *osgi.connection.min* : Defaults to 10. The minimum number of connections that should be kept in the pool

* *osgi.connection.timeout* : Defaults to 30,000 (30 seconds). The maximum time in milliseconds to block when waiting for a database connection

* *osgi.idle.timeout* : Defaults to 180,000 (3 minutes). The time in milliseconds before an idle connection is eligible to be closed.

* *osgi.connection.timeout* : Defaults to 10,800,000 (3 hours). The maximum time in milliseconds that a connection may remain open before being closed.

* *osgi.use.driver* : Defaults to false. If true then use the createDriver method to connect to the database. Cannot be true if *osgi.xa.enabled* is true.


### JDBC properties

The following properties will automatically be passed to the DataSourceFactory if they are present. The list of properties may be overridden using the *aries.jdbc.property.names* property if necessary.

* *databaseName* : The name of the database

* *dataSourceName* : The name of the dataSource that will be created

* *description* : A description of the dataSource being created

* *networkProtocol* : The network protocol to use.

* *portNumber* : The port number to use

* *roleName* : The name of the JDBC role

* *serverName* : The name of the database server

* *user* : The JDBC user

* *password* : The JDBC password

