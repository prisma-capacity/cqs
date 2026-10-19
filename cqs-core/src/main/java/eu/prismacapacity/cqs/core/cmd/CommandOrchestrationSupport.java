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
import eu.prismacapacity.cqs.core.query.QueryValidationException;
import eu.prismacapacity.cqs.core.query.QueryVerificationException;
import eu.prismacapacity.cqs.core.retry.RetryExecutor;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandOrchestrationSupport {

  public <C extends Command, R> R orchestrate(
      @NonNull C message,
      @NonNull Class<?> handlerClass,
      @NonNull CommandStep<C> verifyStep,
      @NonNull CommandInvocation<C, R> handleStep,
      boolean allowNullResponse,
      @NonNull OrchestrationConfiguration configuration,
      @NonNull RetryExecutor executor) {

    try {
      for (var validator : configuration.validators()) {
        validator.validate(message);
      }
      message.validate();
    } catch (CommandValidationException e) {
      throw e;
    } catch (Throwable e) {
      throw new CommandValidationException(e);
    }

    R response =
        executor.execute(
            handlerClass,
            retryCount ->
                configuration
                    .metrics()
                    .timedCommand(
                        handlerClass.getName(),
                        retryCount,
                        () -> {
                          try {
                            verifyStep.perform(message);
                          } catch (Throwable e) {
                            throw e instanceof CommandVerificationException
                                ? (CommandVerificationException) e
                                : new CommandVerificationException(e);
                          }

                          R result;
                          try {
                            result = handleStep.invoke(message);
                          } catch (CommandValidationException
                              | QueryValidationException
                              | CommandVerificationException
                              | QueryVerificationException e) {
                            throw e;
                          } catch (Throwable e) {
                            throw e instanceof CommandHandlingException
                                ? (CommandHandlingException) e
                                : new CommandHandlingException(e);
                          }

                          return result;
                        }));
    if (!allowNullResponse && response == null) {
      throw new CommandHandlingException("Response must not be null.");
    }
    return response;
  }

  @FunctionalInterface
  public interface CommandStep<C extends Command> {
    void perform(@NonNull C message) throws Throwable;
  }

  @FunctionalInterface
  public interface CommandInvocation<C extends Command, R> {
    R invoke(@NonNull C message) throws Throwable;
  }
}
