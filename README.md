# Apache Aries

The Aries project consists of a set of pluggable Java components enabling an enterprise OSGi application programming
model.

See [Apache Aries Website](http://aries.apache.org/).

## Source Code

Most of the code is still at the [Aries svn repo](https://svn.apache.org/repos/asf/aries/).
There is also a [github mirror](https://github.com/apache/aries).

Some of the subproject are already migrated to git:

| Subproject | Apache Git | Github Mirror |
| ---------- | ---------- | ------------- |
| Aries Containers | [apache](https://git-wip-us.apache.org/repos/asf/aries-containers.git) | [github](https://github.com/apache/aries-containers) |
| Aries JAX-RS whiteboard | [apache](https://git-wip-us.apache.org/repos/asf/aries-jax-rs-whiteboard.git) | [github](https://github.com/apache/aries-jax-rs-whiteboard) |
| Aries JPA | [apache](https://git-wip-us.apache.org/repos/asf/aries-jpa.git) | [github](https://github.com/apache/aries-jpa) |
| Aries RSA | [apache](https://git-wip-us.apache.org/repos/asf/aries-rsa.git) | [github](https://github.com/apache/aries-rsa) |

## Build

Most projects can be built using

    mvn clean install

As the Aries svn hosts a lot of different subprojects it makes sense to only
build the specific subproject.
