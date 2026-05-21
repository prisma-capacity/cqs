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
package eu.prismacapacity.cqs.aop.query;

import eu.prismacapacity.cqs.core.metrics.QueryMetrics;
import eu.prismacapacity.cqs.core.query.Query;
import eu.prismacapacity.cqs.core.query.QueryHandler;
import eu.prismacapacity.cqs.core.query.QueryOrchestrationSupport;
import eu.prismacapacity.cqs.spring.retry.RetryUtils;
import javax.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.ClassUtils;

/**
 * Orchestrates the validation/verification/execution handling of a QueryHandler and also maps
 * exceptions if necessary. Using an aspect in this way is kind of a stretch. However, we had
 * aspects, then an abstract class, then aspects again. This is the current incarnation :D
 */
@Aspect
@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public final class QueryHandlerOrchestrationAspect {
  private final Validator validator;

  private final QueryMetrics metrics;

  @Around("execution(* eu.prismacapacity.cqs.core.query.QueryHandler.handle(..))")
  public Object orchestrate(ProceedingJoinPoint joinPoint) {
    Class<?> handlerType = ClassUtils.getUserClass(joinPoint.getTarget().getClass());
    return RetryUtils.withOptionalRetry(
        handlerType,
        retryCount ->
            metrics.timedQuery(handlerType.getName(), retryCount, () -> process(joinPoint)));
  }

  private Object process(ProceedingJoinPoint joinPoint) {
    Query query = (Query) joinPoint.getArgs()[0];
    QueryHandler<Query, Object> target = (QueryHandler<Query, Object>) joinPoint.getTarget();
    return QueryOrchestrationSupport.orchestrate(
        query,
        validator,
        target::validate,
        target::verify,
        ignored -> joinPoint.proceed(),
        metrics::logTimeout);
  }
}
