# CQS Spring AOP orchestration

Add `eu.prismacapacity:cqs-spring-aop:0.0.1-SNAPSHOT` to a Spring application's dependencies,
then import the configuration:

```java
import eu.prismacapacity.cqs.springaop.CqsAopConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(CqsAopConfiguration.class)
public class HandlerConfiguration {}
```

Spring Boot is not required. The module includes `cqs-aop`, `cqs-core`, Spring AOP and Spring Retry support,
and enables class-based proxies. Register your handlers as Spring beans and call their public
`handle` methods through injected references. The aspect supports `CommandHandler`,
`TokenCommandHandler`, `RespondingCommandHandler`, and `QueryHandler` implementations.

By default, orchestration uses `OrchestrationConfiguration.defaults()` and no retries.
Supply an `OrchestrationConfiguration` bean to customize shared validators and metrics.
Annotate handler classes with `@RetryConfiguration` to enable Spring Retry and configure
maximum attempts, the backoff interval in milliseconds, an optional exponential backoff cap,
and exception types to exclude. Exclusions also apply to business exceptions wrapped by
core orchestration. Handlers without the annotation use no retries.
Validation, verification, response checks, metrics and retries delegate to core orchestration.

Handler classes and advised methods must be non-final. Self-invocation and calls on raw
handler instances bypass Spring AOP. Do not wrap advised handlers in core processor factories,
which would apply orchestration twice.

See [spring-aop-example](../spring-aop-example/README.md) for application configuration,
metrics and retry examples.
