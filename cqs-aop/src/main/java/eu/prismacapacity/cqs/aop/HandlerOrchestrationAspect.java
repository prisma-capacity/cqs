/*
 * Copyright © 2026 PRISMA European Capacity Platform GmbH 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.prismacapacity.cqs.aop;

import eu.prismacapacity.cqs.core.OrchestrationConfiguration;
import eu.prismacapacity.cqs.core.cmd.Command;
import eu.prismacapacity.cqs.core.cmd.CommandOrchestrationSupport;
import eu.prismacapacity.cqs.core.cmd.ICommandHandler;
import eu.prismacapacity.cqs.core.query.Query;
import eu.prismacapacity.cqs.core.query.QueryHandler;
import eu.prismacapacity.cqs.core.query.QueryOrchestrationSupport;
import eu.prismacapacity.cqs.core.retry.NoRetryExecutor;
import eu.prismacapacity.cqs.core.retry.RetryExecutor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/** AspectJ adapter for CQS handler implementations; orchestration remains in core. */
@Aspect
@RequiredArgsConstructor
public class HandlerOrchestrationAspect {
  @NonNull private final OrchestrationConfiguration configuration;
  private final Map<Class<?>, RetryExecutor> retryExecutors = new ConcurrentHashMap<>();
  @NonNull private final Function<Class<?>, RetryExecutor> retryExecutorFactory;

  public HandlerOrchestrationAspect(OrchestrationConfiguration configuration) {
    this(configuration, handlerClass -> new NoRetryExecutor());
  }

  @Around(
      value =
          "(execution(public * eu.prismacapacity.cqs.core.cmd.CommandHandler+.handle(eu.prismacapacity.cqs.core.cmd.Command+))"
              + " || execution(public * eu.prismacapacity.cqs.core.cmd.TokenCommandHandler+.handle(eu.prismacapacity.cqs.core.cmd.Command+))"
              + " || execution(public * eu.prismacapacity.cqs.core.cmd.RespondingCommandHandler+.handle(eu.prismacapacity.cqs.core.cmd.Command+)))"
              + " && args(command)",
      argNames = "invocation,command")
  @SuppressWarnings("unchecked") // The matched handle method binds the handler's command type.
  public Object command(ProceedingJoinPoint invocation, Command command) {
    var target = (ICommandHandler<Command>) invocation.getTarget();
    var handlerClass = target.getClass();
    boolean allowsNull =
        ((MethodSignature) invocation.getSignature()).getReturnType() == void.class;
    return CommandOrchestrationSupport.orchestrate(
        command,
        handlerClass,
        target::verify,
        message -> invocation.proceed(),
        allowsNull,
        configuration,
        executorFor(handlerClass));
  }

  @Around(
      value =
          "execution(public * eu.prismacapacity.cqs.core.query.QueryHandler+.handle(eu.prismacapacity.cqs.core.query.Query+))"
              + " && args(query)",
      argNames = "invocation,query")
  @SuppressWarnings("unchecked") // The matched handle method binds the handler's query type.
  public Object query(ProceedingJoinPoint invocation, Query query) {
    var target = (QueryHandler<Query, ?>) invocation.getTarget();
    var handlerClass = target.getClass();
    return QueryOrchestrationSupport.orchestrate(
        query,
        handlerClass,
        target::verify,
        message -> invocation.proceed(),
        configuration,
        executorFor(handlerClass));
  }

  private RetryExecutor executorFor(Class<?> handlerClass) {
    return retryExecutors.computeIfAbsent(handlerClass, retryExecutorFactory);
  }
}
