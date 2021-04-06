# Apache Aries

The Aries project consists of a set of pluggable Java components enabling an enterprise OSGi application programming
model.

See [Apache Aries Website](http://aries.apache.org/).

[![SPI Fly - CI Build](https://github.com/apache/aries/actions/workflows/spi-fly.yml/badge.svg)](https://github.com/apache/aries/actions/workflows/spi-fly.yml)

## Source Code

The sources are now directly availble in [aries github](https://github.com/apache/aries).

Some of the subprojects have their own git repos:

| Subproject |
| ---------- |
| [Aries CDI](https://github.com/apache/aries-cdi) |
| [Aries Component DSL](https://github.com/apache/aries-component-dsl) |
| [Aries Containers](https://github.com/apache/aries-containers) |
| [Aries JAX-RS whiteboard](https://github.com/apache/aries-jax-rs-whiteboard) |
| [Aries JPA](https://github.com/apache/aries-jpa) |
| [Aries RSA](https://github.com/apache/aries-rsa) |
| [Aries Transaction Control](https://github.com/apache/aries-tx-control) |

## Build

Most projects can be built using

    mvn clean install

As the Aries svn hosts a lot of different subprojects it makes sense to only
build the specific subproject.
