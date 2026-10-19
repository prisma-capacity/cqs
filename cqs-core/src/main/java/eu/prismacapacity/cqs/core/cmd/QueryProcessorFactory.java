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
package eu.prismacapacity.cqs.core.cmd;

import eu.prismacapacity.cqs.core.OrchestrationConfiguration;
import eu.prismacapacity.cqs.core.query.Query;
import eu.prismacapacity.cqs.core.query.QueryHandler;
import eu.prismacapacity.cqs.core.query.QueryOrchestrationSupport;
import eu.prismacapacity.cqs.core.retry.RetryExecutor;
import eu.prismacapacity.cqs.core.retry.RetryPolicy;
import lombok.NonNull;

public interface QueryProcessorFactory {
  static <Q extends Query, T> QueryProcessor<Q, T> create(@NonNull QueryHandler<Q, T> handler) {
    return create(handler, OrchestrationConfiguration.defaults(), RetryPolicy.noRetry());
  }

  static <Q extends Query, T> QueryProcessor<Q, T> create(
      @NonNull QueryHandler<Q, T> handler,
      @NonNull OrchestrationConfiguration configuration,
      @NonNull RetryPolicy policy) {
    return create(handler, configuration, (RetryExecutor) policy);
  }

  static <Q extends Query, T> QueryProcessor<Q, T> create(
      @NonNull QueryHandler<Q, T> handler,
      @NonNull OrchestrationConfiguration configuration,
      @NonNull RetryExecutor executor) {
    return query ->
        QueryOrchestrationSupport.orchestrate(
            query, handler.getClass(), handler::verify, handler::handle, configuration, executor);
  }
}
