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
import eu.prismacapacity.cqs.core.cmd.CommandOrchestrationSupport;
import eu.prismacapacity.cqs.core.metrics.CommandMetrics;
import eu.prismacapacity.cqs.spring.retry.RetryUtils;
import javax.validation.Validator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.util.ClassUtils;

@RequiredArgsConstructor
public class DefaultCommandExecutor implements CommandExecutor {

  private final Validator validator;
  private final CommandMetrics metrics;

  @Override
  public <C extends Command, R> R execute(
      @NonNull C cmd,
      @NonNull Class<?> handlerType,
      @NonNull CommandStep<C> validateStep,
      @NonNull CommandStep<C> verifyStep,
      @NonNull CommandInvocation<C, R> handleStep,
      boolean allowNullResponse) {
    Class<?> userHandlerType = ClassUtils.getUserClass(handlerType);
    return RetryUtils.withOptionalRetry(
        userHandlerType,
        retryCount ->
            metrics.timedCommand(
                userHandlerType.getName(),
                retryCount,
                () ->
                    CommandOrchestrationSupport.orchestrate(
                        cmd,
                        validator,
                        validateStep::perform,
                        verifyStep::perform,
                        handleStep::invoke,
                        allowNullResponse)));
  }
}
