<div align="right"><a target="myNextJob" href="https://www.prisma-capacity.eu/careers#job-offers">
    <img class="inline" src="prisma.png" alt="Prisma European Capacity Platform GmbH">
</a></div>

# Prisma CQS

> [!NOTE]
> Work in Progress: This project aims to refactor the legacy https://github.com/prisma-capacity/spring-cqs

Simple abstractions we use to follow the CQS Principle in applications.

![Java CI](https://github.com/prisma-capacity/cqs/workflows/Java%20CI/badge.svg?branch=main)
[![codecov](https://codecov.io/gh/prisma-capacity/spring-cqs/branch/master/graph/badge.svg)](TODO)
[![MavenCentral](https://img.shields.io/maven-central/v/eu.prismacapacity/cqs)](TODO)
<a href="https://www.apache.org/licenses/LICENSE-2.0">
<img class="inline" src="https://img.shields.io/badge/license-ASL2-green.svg?style=flat">
</a>

### Motivation

The [CQS Principle](https://en.wikipedia.org/wiki/Command–query_separation) states that "every method should either be a command that performs an action, or a query that 
returns data to the caller, but not both" to reduce side effects.

In our projects we use abstractions like Query & QueryHandler as well as Command & CommandHandler to follow this
principle. However, there is a bit of fine print here that makes it worthwhile to reuse this in the form of a library:

* a command / query needs to be valid (determined by a MessageValidator), otherwise a Command/Query-ValidationExcption 
  will be thrown
* a command / query validates itself through its mandatory `validate()` method; external validators
  supplement these checks
* a command / query needs to be verified by a mandatory method in the handler, otherwise a
  Command/Query-VerificationException will be thrown
* when a command / query is handled, any exception it may throw is wrapped in a Command/Query-HandlingException

### Usage

Java 17 is required. Use `eu.prismacapacity:cqs-core:0.0.1-SNAPSHOT` as the runtime dependency.
`cqs` is a parent POM. Core has no Spring dependencies. Spring applications can use
[cqs-aop](cqs-aop/README.md) and import `CqsAopConfiguration` to enable handler orchestration.
The separate `spring-aop-example` module demonstrates direct calls to managed handlers.

#### Plain Java

Handlers implement mandatory business verification and business execution. Messages own validation.
Raw `handler.handle(message)` calls perform business execution only. Create a typed processor to
apply orchestration:

```java
import eu.prismacapacity.cqs.core.OrchestrationConfiguration;
import eu.prismacapacity.cqs.core.cmd.*;
import eu.prismacapacity.cqs.core.retry.NoRetryExecutor;
import eu.prismacapacity.cqs.core.retry.SimpleRetryExecutor;

record RenameAccount(String name) implements Command {
  @Override
  public void validate() {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
  }
}

class RenameAccountHandler implements CommandHandler<RenameAccount> {
  @Override
  public void verify(RenameAccount command) {
    // Check permissions and business preconditions here.
  }

  @Override
  public void handle(RenameAccount command) {
    // Persist the new account name here.
  }
}

// Application wiring: share this immutable configuration across processors.
var configuration = OrchestrationConfiguration.defaults();
var handler = new RenameAccountHandler();
CommandProcessor<RenameAccount> processor = CommandProcessorFactory.create(
    handler, configuration, new NoRetryExecutor());
processor.process(new RenameAccount("Example"));
```

All four forms use the same phases:

| Handler | Factory | Processor result |
| --- | --- | --- |
| `CommandHandler<C>` | `CommandProcessorFactory.create(handler, configuration, policy)` | `void` |
| `TokenCommandHandler<C>` | `CommandProcessorFactory.create(handler, configuration, policy)` | `CommandTokenResponse` |
| `RespondingCommandHandler<C, R>` | `CommandProcessorFactory.create(handler, configuration, policy)` | `CommandValueResponse<R>` |
| `QueryHandler<Q, T>` | `QueryProcessorFactory.create(handler, configuration, policy)` | `T` |

Each factory also has `create(handler)` using discovered validators, configured metrics and no retry.
`QueryProcessor` and `QueryProcessorFactory` remain in the `core.cmd` package for compatibility.
Void commands allow no response; the other three forms reject a null response object.
The fields inside `CommandValueResponse` and `CommandTokenResponse` retain their existing nullability.

#### Configuration, retry and metrics

The ordering is:

```text
logical invocation metrics
  external validators, then message.validate() (once)
  attempt loop
    handler.verify() (every attempt)
    handler.handle()
    response contract check
```

`OrchestrationConfiguration` copies the validator list and retains one metrics collaborator.
`defaults()` discovers validators with `ServiceLoader` and snapshots `CqsMetricsSupport` at wiring
time. Without a metrics provider it uses `NoCqsMetrics`. Discovery is not repeated while processing.
With the optional Jakarta Validation API on the classpath, supply a Jakarta implementation as well.
Custom validators, metrics and retry predicates must be thread-safe; the application owns their
lifetime and resources. Close discovered `JakartaMessageValidator` instances at application shutdown,
after processors stop; their validator factories stay open for the entire validator lifetime.
Message validation remains mandatory even with an empty external validator list.

Implement `CqsMetrics` in the application to connect its chosen metrics backend. Core supplies the
metrics contracts, no-op implementation and service discovery; backend dependencies and configuration
belong to the application. Share an application-provided metrics instance through configuration:

```java
var configuration = new OrchestrationConfiguration(
    MessageValidatorFactory.discoverValidators(),
    applicationMetrics);
var renameExecutor = new SimpleRetryExecutor(3, cause -> cause instanceof TransientStorageException);
var rename = CommandProcessorFactory.create(new RenameAccountHandler(), configuration, renameExecutor);
var other = CommandProcessorFactory.create(otherHandler, configuration, new NoRetryExecutor());
```

Factories accept a handler-specific `RetryExecutor`. `SimpleRetryExecutor` provides bounded retries
without backoff; `NoRetryExecutor` executes once and is the factory default.
An application can implement `execute(Class<?>, Function<Integer, T>)` using its preferred retry
library, including Spring Retry, and pass it only to the relevant handler's factory. Retry counts
start at zero. The executor must call the attempt synchronously and propagate its final failure
on exhaustion. The attempt includes verification and handling; validation and logical-invocation
metrics remain outside retries. Non-null response checks run after execution, so a null response
is not retried. Classify CQS failures and their causes explicitly in custom adapters.

Alternatively, the application can supply metrics through `ServiceLoader`. In the application project,
create `src/main/resources/META-INF/services/eu.prismacapacity.cqs.core.metrics.CqsMetrics` containing
the fully qualified provider class name:

```text
com.example.ApplicationMetrics
```

The provider must implement `CqsMetrics` and have a public no-argument constructor. The application
owns its backend initialization and resource lifecycle; initialize any required shared infrastructure
before first using `CqsMetricsSupport`. Then `OrchestrationConfiguration.defaults()` uses the
discovered metrics without additional wiring. Register one provider: discovery selects the first
available provider. Invalid registrations fail discovery rather than silently disabling metrics.
Core deliberately ships no metrics service registration, so its no-op fallback cannot shadow an
application provider. For dependency-injected metrics, call `CqsMetricsSupport.setCqsMetrics(metrics)`
before creating default configurations, or pass metrics directly to `OrchestrationConfiguration`.

`TransientStorageException` above represents an application exception. A retry policy has a positive
maximum attempt count, including the initial attempt, and a predicate selecting verification or handling failures.
Only opt in for operations that can safely repeat. Attempts run immediately without backoff.
There are no annotation overrides or global retry policies. The factories use explicit per-handler policies.

Validation failures terminate immediately, including validation exceptions thrown during verification
or handling. Response-contract failures also terminate without consulting the predicate. Verification
and handling failures are eligible when the predicate selects them; a failed verification skips handling
and the next attempt starts with verification again. Failures are wrapped once in the appropriate phase
exception; existing phase exceptions are preserved. The predicate receives the underlying business cause
after unwrapping verification and handling exceptions, or the phase exception itself if it has no cause.
Errors are never retried. Exhaustion rethrows the last phase exception without an additional wrapper. A predicate that throws terminates
the invocation with its own exception.

A thrown `TimeoutException` becomes `QueryTimeoutException`; an existing `QueryTimeoutException` is
preserved. Query timeouts are eligible only when the configured predicate selects their business cause.
The timeout counter records each timed-out handling attempt, even if a later attempt succeeds.
Timers cover one complete logical invocation, including validation, verification, retries and failure,
using the underlying handler class name. The retained metrics `retryCount` argument is always zero
for this logical timer; it is not an attempt counter. No separate attempt timers are emitted.
Metrics implementations must invoke the supplied callback exactly once and propagate its result or
exception; metrics and retry predicates must not throw during normal use.

#### External adapters

Framework adapters can call `CommandOrchestrationSupport.orchestrate` or
`QueryOrchestrationSupport.orchestrate` with the message, underlying handler class,
verification callback, business invocation callback, configuration and retry executor.
The command adapter also supplies the typed null-response policy.
An AOP adapter should invoke its target continuation through the business callback for each attempt,
avoiding recursive proxy calls or wrapping another processor that would duplicate orchestration.
The [cqs-aop](cqs-aop/README.md) module supplies reusable Spring AOP wiring.
See [spring-aop-example](spring-aop-example/README.md) for a demo client that imports it
and invokes command and query handlers directly.

## Migration

TODO