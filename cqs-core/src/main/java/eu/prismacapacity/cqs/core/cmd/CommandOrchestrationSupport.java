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

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandOrchestrationSupport {

  public <C extends Command, R> R orchestrate(
      @NonNull C cmd,
      @NonNull Validator validator,
      @NonNull CommandStep<C> validateStep,
      @NonNull CommandStep<C> verifyStep,
      @NonNull CommandInvocation<C, R> handleStep,
      boolean allowNullResponse) {
    Set<ConstraintViolation<C>> violations = validator.validate(cmd);
    if (!violations.isEmpty()) {
      throw new CommandValidationException(violations);
    }

    try {
      validateStep.perform(cmd);
    } catch (CommandValidationException e) {
      throw e;
    } catch (Throwable e) {
      throw new CommandValidationException(e);
    }

    try {
      verifyStep.perform(cmd);
    } catch (CommandVerificationException e) {
      throw e;
    } catch (Throwable e) {
      throw new CommandVerificationException(e);
    }

    R result;
    try {
      result = handleStep.invoke(cmd);
    } catch (CommandHandlingException e) {
      throw e;
    } catch (Throwable e) {
      throw new CommandHandlingException(e);
    }

    if (!allowNullResponse && result == null) {
      throw new CommandHandlingException("Response must not be null");
    }

    return result;
  }

  @FunctionalInterface
  public interface CommandStep<C extends Command> {
    void perform(@NonNull C cmd) throws Throwable;
  }

  @FunctionalInterface
  public interface CommandInvocation<C extends Command, R> {
    R invoke(@NonNull C cmd) throws Throwable;
  }
}
