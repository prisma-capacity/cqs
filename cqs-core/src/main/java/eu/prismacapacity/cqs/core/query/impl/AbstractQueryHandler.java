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
package eu.prismacapacity.cqs.core.query.impl;

import eu.prismacapacity.cqs.core.metrics.CqsMetrics;
import eu.prismacapacity.cqs.core.metrics.CqsMetricsSupport;
import eu.prismacapacity.cqs.core.query.*;
import eu.prismacapacity.cqs.core.retry.RetryExecutor;
import eu.prismacapacity.cqs.core.retry.RetrySupport;
import lombok.NonNull;

public abstract class AbstractQueryHandler<Q extends Query, T> implements QueryHandler<Q, T> {

  public final RetryExecutor retryExecutor = RetrySupport.getExecutor();
  public final CqsMetrics cqsMetrics = CqsMetricsSupport.getCqsMetrics();

  @Override
  public abstract void verify(@NonNull Q query) throws QueryVerificationException;

  @Override
  public @NonNull T handle(@NonNull Q query) throws QueryHandlingException {
    return retryExecutor.execute(
        this.getClass(),
        retryCount ->
            cqsMetrics.timedQuery(this.getClass().getName(), retryCount, () -> process(query)));
  }

  private T process(@NonNull Q query) {
    return QueryOrchestrationSupport.orchestrate(
        query, this::validate, this::verify, this::doHandle, cqsMetrics::logTimeout);
  }

  protected abstract T doHandle(@NonNull Q cmd) throws QueryHandlingException;
}
