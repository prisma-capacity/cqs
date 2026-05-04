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
package eu.prismacapacity.spring.cqs.aop.cmd;

import eu.prismacapacity.cqs.spring.retry.RetryUtils;
import eu.prismacapacity.spring.cqs.core.cmd.Command;
import eu.prismacapacity.spring.cqs.core.cmd.CommandHandler;
import eu.prismacapacity.spring.cqs.core.cmd.CommandOrchestrationSupport;
import eu.prismacapacity.spring.cqs.core.cmd.ICommandHandler;
import eu.prismacapacity.spring.cqs.core.metrics.CommandMetrics;
import javax.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.ClassUtils;

/**
 * Orchestrates the validation/verification/execution handling of a (Responding)CommandHandler and
 * also maps exceptions if necessary. Using an aspect in this way is kind of a stretch. However, we
 * had aspects, then an abstract class, then aspects again. This is the current incarnation :D
 */
@Aspect
@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class CommandHandlerOrchestrationAspect {

  private static final String PC_COMMAND_HANDLER =
      "execution(* eu.prismacapacity.spring.cqs.core.cmd.CommandHandler+.handle(..))";
  private static final String PC_RESPONDING_COMMAND_HANDLER =
      "execution(* eu.prismacapacity.spring.cqs.core.cmd.RespondingCommandHandler+.handle(..))";
  private static final String PC_TOKEN_COMMAND_HANDLER =
      "execution(* eu.prismacapacity.spring.cqs.core.cmd.TokenCommandHandler+.handle(..))";

  private final Validator validator;
  private final CommandMetrics metrics;

  @Around(
      PC_COMMAND_HANDLER
          + " || "
          + PC_RESPONDING_COMMAND_HANDLER
          + " || "
          + PC_TOKEN_COMMAND_HANDLER)
  public Object orchestrate(ProceedingJoinPoint joinPoint) {
    Class<?> handlerType = ClassUtils.getUserClass(joinPoint.getTarget().getClass());
    return RetryUtils.withOptionalRetry(
        handlerType,
        retryCount ->
            metrics.timedCommand(handlerType.getName(), retryCount, () -> process(joinPoint)));
  }

  private Object process(ProceedingJoinPoint joinPoint) {
    Command cmd = (Command) joinPoint.getArgs()[0];
    Object target = joinPoint.getTarget();
    ICommandHandler<Command> commandHandler = (ICommandHandler<Command>) target;
    return CommandOrchestrationSupport.orchestrate(
        cmd,
        validator,
        commandHandler::validate,
        commandHandler::verify,
        ignored -> joinPoint.proceed(),
        target instanceof CommandHandler);
  }
}
