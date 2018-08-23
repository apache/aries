# osgi-component-dsl

A lightweight functional DSL to interact with OSGi registry. 

This DSL provides a set of operations to fetch service references and
configurations from OSGi, as well as to register services back into OSGi
registry. It also provides a way to create new custom operations.

   *This documentation is still work in progress*

## Why should I use it?

One of the benefits of using this DSL is that operations are automatically bound
to the service or configuration intances that triggered their execution. This
allows them to automatically undo (or clean) when those instances are no longer
available. This way the user of the DSL does not need to manually account for
the tracked instances and ensure the proper cleaning, which is a source of
mistakes. For those cases in which manual assistance is needed, such as side
effects management, the DSL facilitates specifying the undo operation together
with the side effect. This allows for better reutilization.

The DSL runs without the need for any additional runtime.

## Quick start

The foundation of the DSL is one type `OSGi<T>`. There exist several static
functions defined on the `OSGi` class that provide us with instances of the
type. Let's start getting a reference to a service:

    OSGi<Service> program = OSGi.service(OSGi.serviceReferences(Service.class));

if we allow static imports from `OSGi` class we can type the former as:

    OSGi<Service> services = service(serviceReferences(Service.class));
	OSGi<Dictionary<String, ?>> configurations = configurations("factory-pid");
	

### Combining operations

Once we have instances of `OSGi` we can combine them. We can use `flatMap` to
specify that one operation depends on a previous one. For example we could need
to specify a filter for the services that comes in the configuration:

    OSGi<Service> services = configurations("factory-pid").flatMap(conf ->
		service(serviceReferences(Service.class, conf.get("service.filter").toString()))
	);

we can also register instances. For this purpose let's create a new class that
will hold instances of both `Dictionary<String, ?>` and `Service`. 

    class Holder {
		
		Dictionary<String, ?> properties;
		Service service;

		public Holder(Dictionary<String, ?> properties, Service service) {
			this.properties = properties;
			this.service = service;
		}
		
	}

and we can register instances of that class with:
    
	OSGi<ServiceRegistration<Holder>> program = configurations("factory-pid").flatMap(conf ->
			service(serviceReferences(Service.class, conf.get("service.filter").toString())).flatMap(service ->
				register(Holder.class, new Holder(conf, service), new HashMap<>())));

in this example we are tracking factory configurations from pid
`factory-pid`. For each configuration factory that's there, or that's created in
the future, we get the property `service.filter` and use it's value to track
services of type `Service` that match the filter in the configuration. Then, for
each service that matches that filter and each configuration factory
combination we register one `Holder` instance. 

If any of the configuration factories goes away, or any of the tracked services
goes away, the corresponding `Holder` instances will be unregistered
automatically, since the DSL tracks the effects each instance has produced.

### Running the _program_

In the previous section we have gone through combining different operations to
produce new instances. These instances are values that describe how we are going
to interact with OSGi and are immutable. You can use them to produce new
programs like reusable components. 

Once you have a complete specification you can run it passing a `BundleContext`
to it:

    OSGi<T> program;
	BundleContext bundleContext;

	OSGiResult result = program.run(bundleContext);

the `OSGiResult` instance references the execution of the program. To stop and
clean it we call:

    result.close()

### Combinations without dependency

we can also specify combinations when there exist no dependencies between the
operations. For instance we can declare we want to register a `Holder` for every
configuration + service combination using `OSGi.combine`:

    OSGi<Holder> holders = combine(Holder::new, configurations("factory.pid"), services(Service.class));


## Predefined Operations

the DSL comes with some common operations defined on the OSGi type. All these
operations produce values of a type and keep track of the next operations. When
a tracked value goes away all the associated operations will be cleaned as well.

### Configurations

There are two operations to deal with `Configuration Admin` configurations:
`OSGi.configuration` to deal with singleton configurations and
`OSGi.configurations` to deal with factory configurations. 

### Services

`OSGi.serviceReferences` is a set of overloaded functions that return operations
of type `OSGi<CachingServiceReference>`: 

    OSGi<CachingServiceReference<T>> serviceReferences(Class<T> clazz)

	OSGi<CachingServiceReference<Object>> serviceReferences(String filterString)

	OSGi<CachingServiceReference<T>> serviceReferences(Class<T> clazz, String filterString)

	OSGi<CachingServiceReference<T>> serviceReferences(
		Class<T> clazz, String filterString,
		Refresher<? super CachingServiceReference<T>> onModified)

	OSGi<CachingServiceReference<T>> serviceReferences(
		Class<T> clazz, Refresher<? super CachingServiceReference<T>> onModified)

	OSGi<CachingServiceReference<Object>> serviceReferences(
		String filterString,
		Refresher<? super CachingServiceReference<Object>> onModified)
		
#### CachingServiceReference<T>

 This class is an explicit wrapper around `ServiceReference` (it DOES NOT
 implement `ServiceReference`) and provides methods to access underlying
 `ServiceReference` properties and caches them so future access to the
 same properties return the same values.

 Property values are cached *on demand*. Values that have never been
 queried through the method are not cached.

 Properties that did not exist when queried will no longer exist even though
 they were available at a later time in the underlying `ServiceReference`.
 
 This class was introduced because `ServiceReference` is mutable, which made it
 very difficult to operate on it within the DSL in a safe manner. 
 
#### Refresher<T>

This class is just an alias for `Predicate<T>`. `serviceReferences` operations
will invoke the `Refresher` to know if the modified instance needs to be
retracted and reintroduced in the execution. If no refresher is passed
`serviceReferences` will check `CachingServiceReference.isDirty()`.

#### Getting services from `ServiceReference`

There are two sets of overloaded operations that allow to get services from
`CachingServiceReference` or `ServiceReference`: `service` and `prototypes`. 

`service` will get services invoking `bundleContext.getService` and unget them
using `bundleContext.ungetServices`. `prototypes`, on the other hand, will
produce a `ServiceObjects` instance. It will be the responsability of the user
to properly balance `getService` and `ungetService` calls to `ServiceObjects`.

### BundleContext

`bundleContext` operation will produce the `BundleContext` in use by that piece
of program. It will normally be the one used to invoke `OSGi.run` unless it is
changed using `OSGi.changeContext(BundleContext bundleContext, OSGi<T> program)`
for that particular program.

### Just and nothing

`just` set of operations allows to wrap any value inside a `OSGi` type. It is
overloaded to support `Supplier` and `Collection`. If a collection is passed it
will produce the elements of the list in the order given by the collection. 

`nothing`, as it name suggests, is a termination operation. Anything depending
on the result of `nothing` operation should never be executed. 

### Service Registration

One common operation when using OSGi is service registration. For that purpose
the library offers `register` set of functions. When the associated instances
are retracted, `register` set of functions also unregister their instances as a
result.
    
	OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, ServiceFactory<T> service, 
		Map<String, Object> properties)
	

	OSGi<ServiceRegistration<?>> register(
		String[] classes, Object service, Map<String, ?> properties)

	OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, Supplier<T> service, 
		Supplier<Map<String, ?>> properties)

	OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, ServiceFactory<T> service, 
		Supplier<Map<String, ?>> properties)

	OSGi<ServiceRegistration<?>> register(
		String[] classes, Supplier<Object> service, 
		Supplier<Map<String, ?>> properties)

## Combinators

### All

`OSGi<T> all(OSGi<T> ... programs)` will execute all given programs and produce
all the elements that the given programs produce. 

### Coalesce 

`OSGi<T> coalesce(OSGi<T> ... programs)`, just as its homonymous SQL function,
`coalesce` will produce the value of the first producing program that is being
given as argument, from left to right. Since OSGi is a dynamic environment
`coalesce` will retract and reintroduce instances when needed. For example:

	OSGi<Dictionary<String, ?>> props = coalesce(
		configuration("some.config.pid"), 
		just(Hashtable::new)
	);

this operation will return an empty Hashtable while no configuration is
available. If, at any moment, there is a configuration available the operation
will retract the empty `Hashtable` and introduce the incoming configuration. If,
at any moment, the configuration is deleted, the operation will retract the
`Dictionary` associated with that configuration and reintroduce the empty
`Hashtable`.

This is very useful to model defaults or to model different level for preferred
services, from more specific to more general. 

### Combine

`OSGi.combine` set of functions is the `Applicative` implementation for `OSGi`
class. It produces the result of the invocation to the given function with all
the combinations of the values produced by the given operations.

### Once

`OSGi.once` will just let the first instance produced by the given operation to
pass. Further instances won't be published. Actually `once` does not support any
order so it can't decide that any instance is more suitable than any other.

`once` will update a counter with the instances that it encounters and that are
retracted. It won't retract the instance it produces until all the instances it
has _seen_ are gone.

## License

  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. 
