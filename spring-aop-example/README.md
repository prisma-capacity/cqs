# Spring AOP client example

This module is a Spring Boot demo application using [cqs-aop](../cqs-aop/README.md).
The reusable aspect and its Spring configuration live in `cqs-aop`; core remains framework-independent.

`SpringAopExample` receives managed handler references through constructor injection;
its `main` method calls them directly:

```java
example.createAccountHandler.handle(new CreateAccount(1, "Alice"));
System.out.println(example.findAccountHandler.handle(new FindAccount(1)));
```

`SpringAopExample` uses `@SpringBootApplication` to discover the `@Component` handlers and
in-memory repository. Boot enables AOP class proxies through `spring-boot-starter-aop`.
`ExampleConfiguration` imports `CqsAopConfiguration` and supplies the shared orchestration configuration.
The imported configuration registers the policy-configured aspect. The aspect matches public `handle` methods on implementations of
`CommandHandler`, `TokenCommandHandler`, `RespondingCommandHandler`, and `QueryHandler`.
New handler implementations need no additional advice. It delegates to the existing command/query orchestrators,
using the underlying target's `verify` method and `ProceedingJoinPoint.proceed()` as the business
invocation callback. Spring's continuation can be invoked again for each retry without re-entering
the same advice. No processor factory or second orchestration pipeline is involved.

Message validation runs once before attempts. Verification runs before each handling attempt.
The void command allows a null response; the query must return an account. Phase exceptions,
retry eligibility and response checks are supplied by core. Both handlers default to no retry;
annotate a handler class with `eu.prismacapacity.cqs.aop.RetryConfiguration` to enable retries.
For example:

```java
@RetryConfiguration(maxAttempts = 3, interval = 20)
```

Handlers without the annotation use no retry. Policies are class-scoped, so beans of the same
implementation class share the configuration. One shared
`OrchestrationConfiguration` uses no external validators and the shared `MicrometerCqsMetrics`
component; message validation still runs.

## Metrics

The module depends on `micrometer-core`, with its version managed by Spring Boot.
`ExampleConfiguration` provides an in-memory `SimpleMeterRegistry`; the discovered
`MicrometerCqsMetrics` component implements core's `CqsMetrics` interface and is injected into
orchestration configuration. Core has no Micrometer dependency.

- `cqs.command.duration`: timer tagged with the command handler's class name (`handler`).
- `cqs.query.duration`: timer tagged with the query handler's class name (`handler`).
- `cqs.query.timeouts`: counter incremented for each timed-out query handling attempt.

Timers record both successful and failed logical invocations, including validation, verification,
and all retries. Their counts are invocation counts, not attempt counts. The current core timing
hooks pass zero for `retryCount`; this adapter intentionally does not expose it as a retry metric.
The timeout callback has no handler identity, so the timeout counter is application-wide.
Meters are created on first use; no message ids or payloads become tags.

To inspect a timer after calling the create handler, inject `MeterRegistry` and use:

```java
long invocations = meterRegistry.get("cqs.command.duration")
    .tag("handler", CreateAccountHandler.class.getName())
    .timer().count();
```

The demo registry stores metrics locally and closes with the application context. It does not
publish metrics or expose an HTTP endpoint. Replace its bean with the application's chosen
registry when exporting metrics; the CQS adapter and handlers remain the same.

## Run

Compile the example and its CQS dependencies from the repository root:

```sh
mvn -pl spring-aop-example -am -Dmaven.test.skip=true compile
```

Run `eu.prismacapacity.cqs.examples.springaop.SpringAopExample` in your IDE with the example
module's runtime classpath. Its `main` starts Spring Boot, calls the injected handlers and closes the
context. Expected output includes:

```text
Account[id=1, name=Alice]
```

## Integration tests

`HandlerOrchestrationIntegrationTest` uses JUnit Jupiter and `@SpringBootTest` to discover
`SpringAopExample` automatically. Handlers and storage are injected with `@Autowired`.
`@DirtiesContext` gives each test fresh in-memory storage. Tests do not invoke `main`, so the
demo account is not created during test startup.

The welcome-email command, handlers, stub gateway, and transient exception live alongside the
other example classes in main sources and are discovered through component scanning. The gateway
exposes failure controls and counters for the single-threaded demo. `SendWelcomeEmailHandler`
uses `@RetryConfiguration` to select a Spring `RetryTemplate` with three attempts maximum,
excluding command/query validation exceptions and command verification exceptions.
Transient email-service failures are retried; existing handlers
retain their default single-attempt behavior. The adapter supplies zero-based retry counts to the
core callback. Validation runs once, verification runs per attempt, and exhaustion retains the CQS
handling exception. Spring Retry is supplied by `cqs-aop`, with no second retry aspect.
Cases cover recovery, exhaustion, validation/verification failures, and an unannotated
handler throwing the same transient exception without retrying.

```sh
mvn -pl spring-aop-example -am -Dtest=HandlerOrchestrationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## Proxy boundary

Calls must use the Spring-managed handler references. Raw handler instances and self-invocation
bypass the aspect, as do calls before proxy initialization. Handler classes and `handle` methods
must remain non-final for class proxies. Only `handle` is advised; calling `verify` directly does
not trigger orchestration. The pointcuts cover all four CQS handler forms across application packages.
Only void command methods permit a null result; token commands, responding commands, and queries
retain core non-null response checks. Do not wrap the managed references in processor
factories, which would introduce a second pipeline.

The repository is illustrative in-memory storage, not a persistence or transaction implementation.
