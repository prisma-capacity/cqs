<div align="right"><a target="myNextJob" href="https://www.prisma-capacity.eu/careers#job-offers">
    <img class="inline" src="prisma.png" alt="Prisma European Capacity Platform GmbH">
</a></div>

# Prisma CQS

Simple abstractions we use to follow the CQS Principle in applications.

![Java CI](https://github.com/prisma-capacity/cqs/workflows/Java%20CI/badge.svg?branch=main)
[![codecov](https://codecov.io/gh/prisma-capacity/spring-cqs/branch/master/graph/badge.svg)](TODO)
[![MavenCentral](https://img.shields.io/maven-central/v/eu.prismacapacity/cqs)](TODO)
<a href="https://www.apache.org/licenses/LICENSE-2.0">
<img class="inline" src="https://img.shields.io/badge/license-ASL2-green.svg?style=flat">
</a>

### Motivation

The [CQS Principle](https://en.wikipedia.org/wiki/Command–query_separation) states that "every method should either be a command that performs an action, or a query that 
returns data to the caller, but not both." in order to reduce side-effects.

In our projects we use abstractions like Query & QueryHandler as well as Command & CommandHandler to follow this
principle. However, there is a bit of fine print here that makes it worthwhile to reuse this in form of a library:

* a command / a query needs to be valid (as in java.validation valid), otherwise a Command/Query-ValidationExcption will
  be thrown
* a command / a query needs to be valid (determined by an optional message on the handler), otherwise a
  Command/Query-ValidationExcption will be thrown
* a command / a query needs to be verified by a mandatory method in the handler the is expected to throw a
  Command/Query-VerificationException
* when a command / a query is handled, any exception it may throw is to be wrapped in a Command/Query-Handling Exception

### Usage

TODO

#### Maven

`cqs` is a parent-only artifact (`packaging: pom`). For Spring Boot applications, use one of the
starter modules:

````
    <dependency>
      <groupId>eu.prismacapacity</groupId>
      <artifactId>cqs-spring-boot-starter</artifactId>
      <version><!-- put the desired version in here--></version>
    </dependency>
````

or, for the AOP based integration:

````
    <dependency>
      <groupId>eu.prismacapacity</groupId>
      <artifactId>cqs-aop-spring-boot-starter</artifactId>
      <version><!-- put the desired version in here--></version>
    </dependency>
````

For non-Spring usage, use `cqs-core` as dependency:

````
    <dependency>
      <groupId>eu.prismacapacity</groupId>
      <artifactId>cqs-core</artifactId>
      <version><!-- put the desired version in here--></version>
    </dependency>
````

#### Configuration

TODO

#### Example

TODO

#### Configure a retry behaviour for Command and Query handlers

TODO

## Migration

TODO
