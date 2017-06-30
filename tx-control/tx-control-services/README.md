Apache Aries OSGi Transaction Control Service implementations
------------------------------------------------------------

This project contains modules which implement the OSGi Transaction Control Service.

The Transaction Control Service is defined in Chapter 147 of the OSGi R7 specification. This specification is not yet final, but public drafts of this specification are available from the OSGi Alliance.

Given that the RFC is non-final the OSGi API declared in this project is subject to change at any time up to its official release. Also the behaviour of this implementation may not always be up-to-date with the latest wording in the RFC. The project maintainers will, however try to keep pace with the RFC, and to ensure that the implementations are compliant with any OSGi specifications that result from the RFC.

# Modules

The following implementations are available:

1. tx-control-service-local :- A purely local transaction control service implementation. This can be used with any resource-local capable ResourceProvider
2. tx-control-service-xa :- An XA-capable transaction control service implementation based on the Geronimo Transaction Manager. This can be used with XA capable resources, or with local resources. Local resources will make use of the last-participant gambit.


## Which modules should I use?

If you wish to use entirely lightweight, resource-local transactions then it is best to pair the tx-control-service-local with a local resource provider implementation.

If two-phase commit is needed across multiple resources then the tx-control-service-xa must be used with an XA capable resource provider implementation.

It is not advised to use multiple Transaction Control Service implementations at the same time. This will be confusing, and may lead to problems if different parts of your application bind to different service implementations. If you do choose to deploy multiple Transaction Control services then please ensure that the different parts of your application target the correct service implementation.

# Using the Transaction Control Service

The Transaction Control service is used in conjunction with one or more ResourceProvider services to provide scoped resource access. 

## Accessing the TransactionControl service

The TransactionControl implementation is registered as an OSGi service in the service registry. This can easily be injected using various OSGi frameworks. For example using Declarative Services:

    @Reference
    private TransactionControl txControl;


## Controlling the transaction lifecycle

When using the Transaction Control service your code is running in one of three scopes:

1. Unscoped :- In this case there is no scope associated with the thread. Resources cannot be used and will throw Exceptions.
2. No Transaction Scope :- Resources may be used in this scope, but there is no transaction associated with the thread. This means that resources will be cleaned up at the end of the scope but no work will be committed. Changes in this scope may not be atomic.
3. Transaction Scope := In this case there is an active transaction and resources will participate in it. Updates and reads will be atomic, and will all roll back in the event of a failure.


### Starting a Transaction or scope

To start a transaction simply pass a `Callable` to the `required` method of the TransactionControl service. The Callable will be run scoped in a transaction

    txControl.required(() -> {
            // Use a resource in here
        });

To start a scope without starting a transaction simply pass a `Callable` to the `notSupported` method of the TransactionControl service. The Callable will be run within a non-transactioonal scope

    txControl.notSupported(() -> {
        // Use a resource in here, but it won't be part of a transaction
    });


### Finishing a Transaction or Scope

A scope (including any related transaction) finishes when the `Callable` completes. Any resources used in the scope will be cleaned up automatically so there is no need to close them. If the scope is transactional then it will either:

 * Commit the transaction. This will occur if the `Callable` returns normally, and the transaction has not been marked for rollback.

  or

 * Roll back the transaction. This will occur if the `Callable` exits with an `Exception` or if the transaction has been marked for rollback by calling `setRollbackOnly()`.

Marking a transaction for rollback:

    txControl.required(() -> {
        // This call means that the transaction must roll back.
        txControl.setRollbackOnly();
    });


### Transaction Inheritance

Transactions may be inherited:

    txControl.required(() -> {
    
            // A transaction is running here
    
            txControl.required(() -> {
                    // The same transaction is in force here
                });

        });

Transactions may be suspended:

    txControl.required(() -> {
    
            // A transaction is running here
    
            txControl.notSupported(() -> {
                    // No transaction in here
                });

            // The original transaction is in force
        });

Transactions may be nested:

    txControl.required(() -> {
        
        // A transaction is running here
        
        txControl.requiresNew(() -> {
                // A new transaction is in force here
            });

    });

### Advanced usage

For more advanced usage see the API JavaDoc, and read the Transaction Control Service Specification
