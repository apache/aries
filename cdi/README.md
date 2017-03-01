# Aries CDI Integration

This is an implementation of [OSGi RFC 0193](https://github.com/osgi/design/blob/master/rfcs/rfc0193/rfc-0193-CDI-Integration.pdf).

## License

[Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Building From Source

The build uses maven so it should look pretty familiar to most developers.

`mvn clean install`

## Pre-built runtime

This repository contains an [OSGi enRoute](http://enroute.osgi.org/) based, pre-assembled executable jar providing a complete runtime for you to just drop in your CDI bundles. It comes preconfigured with logging, Gogo shell and Felix SCR.

Once you've completed a successfull build, you should be able to execute the command:

`java -jar cdi-itests/target/cdi-executable.jar`

and be presented with a gogo shell prompt ready for you to install a CDI bundle.
