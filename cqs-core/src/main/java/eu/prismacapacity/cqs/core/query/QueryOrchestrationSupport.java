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
package eu.prismacapacity.cqs.core.query;

import eu.prismacapacity.cqs.core.OrchestrationConfiguration;
import eu.prismacapacity.cqs.core.cmd.CommandValidationException;
import eu.prismacapacity.cqs.core.cmd.CommandVerificationException;
import eu.prismacapacity.cqs.core.retry.RetryExecutor;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QueryOrchestrationSupport {

  public <Q extends Query, T> T orchestrate(
      @NonNull Q message,
      @NonNull Class<?> handlerClass,
      @NonNull QueryStep<Q> verifyStep,
      @NonNull QueryInvocation<Q, T> handleStep,
      @NonNull OrchestrationConfiguration configuration,
      @NonNull RetryExecutor executor) {
    try {
      for (var validator : configuration.validators()) {
        validator.validate(message);
      }
      message.validate();
    } catch (QueryValidationException e) {
      throw e;
    } catch (Throwable e) {
      throw new QueryValidationException(e);
    }

    T response =
        executor.execute(
            handlerClass,
            retryCount ->
                configuration
                    .metrics()
                    .timedQuery(
                        handlerClass.getName(),
                        retryCount,
                        () -> {
                          try {
                            verifyStep.perform(message);
                          } catch (Throwable e) {
                            throw e instanceof QueryVerificationException
                                ? (QueryVerificationException) e
                                : new QueryVerificationException(e);
                          }

                          T result;
                          try {
                            result = handleStep.invoke(message);
                          } catch (CommandValidationException
                              | QueryValidationException
                              | CommandVerificationException
                              | QueryVerificationException e) {
                            throw e;
                          } catch (TimeoutException e) {
                            configuration.metrics().logTimeout();
                            throw new QueryTimeoutException(e);
                          } catch (QueryTimeoutException e) {
                            configuration.metrics().logTimeout();
                            throw e;
                          } catch (Throwable e) {
                            throw e instanceof QueryHandlingException
                                ? (QueryHandlingException) e
                                : new QueryHandlingException(e);
                          }

                          return result;
                        }));
    if (response == null) {
      throw new QueryHandlingException("Response must not be null.");
    }
    return response;
  }

  @FunctionalInterface
  public interface QueryStep<Q extends Query> {
    void perform(@NonNull Q message) throws Throwable;
  }

  @FunctionalInterface
  public interface QueryInvocation<Q extends Query, T> {
    T invoke(@NonNull Q message) throws Throwable;
  }
}
