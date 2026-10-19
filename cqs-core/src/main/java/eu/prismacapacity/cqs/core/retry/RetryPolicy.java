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
package eu.prismacapacity.cqs.core.retry;

import eu.prismacapacity.cqs.core.cmd.CommandHandlingException;
import eu.prismacapacity.cqs.core.cmd.CommandValidationException;
import eu.prismacapacity.cqs.core.query.QueryHandlingException;
import eu.prismacapacity.cqs.core.query.QueryValidationException;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.NonNull;

/** Immutable handler policy. The predicate receives the business cause, when available. */
public record RetryPolicy(int maxAttempts, @NonNull Predicate<Throwable> retryOn)
    implements RetryExecutor {
  public RetryPolicy {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be at least one");
    }
  }

  public static RetryPolicy noRetry() {
    return new RetryPolicy(1, failure -> false);
  }

  @Override
  public <T> T execute(@NonNull Class<?> handler, @NonNull Function<Integer, T> attempt) {
    for (int retryCount = 0; ; retryCount++) {
      try {
        return attempt.apply(retryCount);
      } catch (RuntimeException failure) {
        if (shouldNotRetry(failure, retryCount + 1)) {
          throw failure;
        }
      }
    }
  }

  /** Returns whether to stop retrying. attempt is one-based; Errors are never eligible. */
  public boolean shouldNotRetry(@NonNull Throwable failure, int attempt) {
    Throwable cause = failure;
    while ((cause instanceof CommandHandlingException || cause instanceof QueryHandlingException)
        && cause.getCause() != null
        && cause.getCause() != cause) {
      cause = cause.getCause();
    }
    return attempt >= maxAttempts
        || cause instanceof Error
        || cause instanceof CommandValidationException
        || cause instanceof QueryValidationException
        || !retryOn.test(cause);
  }
}
