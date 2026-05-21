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
package eu.prismacapacity.cqs.spring.query;

import eu.prismacapacity.cqs.core.query.Query;
import eu.prismacapacity.cqs.core.query.QueryHandlingException;
import lombok.NonNull;

public interface QueryExecutor {

  <Q extends Query, T> T execute(
      @NonNull Q query,
      @NonNull Class<?> handlerType,
      @NonNull QueryStep<Q> validateStep,
      @NonNull QueryStep<Q> verifyStep,
      @NonNull QueryInvocation<Q, T> handleStep)
      throws QueryHandlingException;

  @FunctionalInterface
  interface QueryStep<Q extends Query> {
    void perform(@NonNull Q query) throws Throwable;
  }

  @FunctionalInterface
  interface QueryInvocation<Q extends Query, T> {
    T invoke(@NonNull Q query) throws Throwable;
  }
}
