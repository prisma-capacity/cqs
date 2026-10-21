# CQS AspectJ orchestration

This module contains the framework-independent `HandlerOrchestrationAspect` and
`RetryConfiguration` annotation. It depends on `cqs-core` and AspectJ, with no Spring dependencies.

Create the aspect with an `OrchestrationConfiguration` and register it with your AOP runtime.
The single-argument constructor uses no retries. Supply a
`Function<Class<?>, RetryExecutor>` as the second constructor argument to select a retry
executor for each handler class; executors are cached by handler class.

The aspect supports `CommandHandler`, `TokenCommandHandler`, `RespondingCommandHandler`,
and `QueryHandler` implementations, delegating validation, verification, response checks,
metrics and retries to core orchestration.

For Spring configuration and annotation-based retries using Spring Retry, use
[cqs-spring-aop](../cqs-spring-aop/README.md).
