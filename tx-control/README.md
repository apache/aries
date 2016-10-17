Sample OSGi Transaction Control Service implementation
------------------------------------------------------

This set of modules is a prototype implementation of the OSGi Transaction Control Service and related services, such as JDBC resource provision.

The Transaction Control Service (RFC-221) is an in-progress RFC publicly available from the OSGi Alliance: https://github.com/osgi/design/blob/master/rfcs/rfc0221/rfc-0221-TransactionControl.pdf

Given that the RFC is non-final the OSGi API declared in this project is subject to change at any time up to its official release. Also the behaviour of this implementation may not always be up-to-date with the latest wording in the RFC. The project maintainers will, however try to keep pace with the RFC, and to ensure that the implementations are compliant with any OSGi specifications that result from the RFC.

# Modules

The following modules are available for use in OSGi

1. tx-control-service-local :- A purely local transaction control service implementation. This can be used with any resource-local capable ResourceProvider
2. tx-control-service-xa :- An XA-capable transaction control service implementation based on the Geronimo Transaction Manager. This can be used with XA capable resources, or with local resources. Local resources will make use of the last-participant gambit.
3. tx-control-provider-jdbc-local := A JDBC resource provider that can integrate with local transactions. The JDBCConnectionProviderFactory service may be used directly, or a service may be configured using the _org.apache.aries.tx.control.jdbc.local_ pid
4. tx-control-provider-jdbc-xa := A JDBC resource provider that can integrate with local or XA transactions. The JDBCConnectionProviderFactory service may be used directly, or a service may be configured using the _org.apache.aries.tx.control.jdbc.xa_ pid


## Which modules should I use?

If you wish to use entirely lightweight, resource-local transactions then it is best to pair the tx-control-service-local and tx-control-provider-jdbc-local bundles.

If two-phase commit is needed across multiple resources then the tx-control-service-xa and tx-control-provider-jdbc-xa bundles should be used.

DO NOT use both tx-control-service-xa and tx-control-service-local at the same time. This will be confusing, and will lead to problems if different parts of the runtime bind to different service implementations.

There is also no reason to use the tx-control-provider-jdbc-local in addition to the tx-control-provider-jdbc-xa service. Using both together is not typically harmful, however the tx-control-provider-jdbc-xa bundle supports all of the same features as the tx-control-provider-jdbc-local bundle.

# Using the Transaction Control Service

The Transaction Control service is used in conjunction with one or more ResourceProvider services to provide scoped resource access.

## Creating a resource

Preparing a resource for use is very simple. Connect the ResourceProvider to a TransactionControl, and the thread-safe created resource can then be used in any ongoing scoped work.

    @Reference
    TransactionControl txControl;

    @Reference
    ResourceProvider<MyResource> provider;

    MyResource resource;

    @Activate
    void start() {
        resource = provider.getResource(txControl);
    }

    /**
     * Persists data inside a transaction
     */
    public void persistData(MyData data) {
        txControl.required(() -> resource.persist(data));
    } 


### Specialised resource interfaces

The OSGi service registry does not natively handle genericized types, so the Transaction Control RFC defines specialised interface types for common resource types, for example the JDBCConnectionProvider.

    @Reference
    TransactionControl txControl;

    @Reference
    JDBCConnectionProvider provider;

    Connection conn;

    @Activate
    void start() {
        conn = provider.getResource(txControl);
    }

    public void findUserName(String id) {
        txControl.required(() -> {
                // Use the connection in here
            });
    } 

## Controlling the transaction lifecycle

When using the Transaction Control service your code is running in one of three scopes:

1. Unscoped :- In this case there is no scope associated with the thread. Resources cannot be used and will throw Exceptions.
2. No Transaction Scope :- Resources may be used in this scope, but there is no transaction associated with the thread. This means that resources will be cleaned up at the end of the scope but no work will be committed. Changes in this scope may not be atomic.
3. Transaction Scope := In this case there is an active transaction and resources will participate in it. Updates and reads will be atomic, and will all roll back in the event of a failure.


### Starting a Transaction

To start a transaction simply pass a Callable to the required method of the TransactionControl service. The Callable will be run scoped in a transaction

    txControl.required(() -> {
            // Use the connection in here
        });

Transactions may be nested:

    txControl.required(() -> {
    
            // Use the connection in here
    
            txControl.required(() -> {
                    // Nested transaction in here
                });

        });

Transactions may be or suspended:

    txControl.required(() -> {
    
            // Use the connection in here
    
            txControl.notSupported(() -> {
                    // Nested transaction in here
                });

        });

### Advanced usage

For more advanced usage see the API JavaDoc, and read the RFC (https://github.com/osgi/design/blob/master/rfcs/rfc0221/rfc-0221-TransactionControl.pdf)