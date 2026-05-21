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
package eu.prismacapacity.cqs.spring.cmd;

import eu.prismacapacity.cqs.core.cmd.Command;
import eu.prismacapacity.cqs.core.cmd.CommandHandlingException;
import lombok.NonNull;

public interface CommandExecutor {

  <C extends Command, R> R execute(
      @NonNull C cmd,
      @NonNull Class<?> handlerType,
      @NonNull CommandStep<C> validateStep,
      @NonNull CommandStep<C> verifyStep,
      @NonNull CommandInvocation<C, R> handleStep,
      boolean allowNullResponse)
      throws CommandHandlingException;

  @FunctionalInterface
  interface CommandStep<C extends Command> {
    void perform(@NonNull C cmd) throws Throwable;
  }

  @FunctionalInterface
  interface CommandInvocation<C extends Command, R> {
    R invoke(@NonNull C cmd) throws Throwable;
  }
}
