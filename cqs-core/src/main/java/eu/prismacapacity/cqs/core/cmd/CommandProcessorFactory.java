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
import eu.prismacapacity.cqs.core.retry.RetryExecutor;
import eu.prismacapacity.cqs.core.retry.RetryPolicy;
import lombok.NonNull;

public interface CommandProcessorFactory {

  static <C extends Command> CommandProcessor<C> create(@NonNull CommandHandler<C> handler) {
    return create(handler, OrchestrationConfiguration.defaults(), RetryPolicy.noRetry());
  }

  static <C extends Command> CommandProcessor<C> create(
      @NonNull CommandHandler<C> handler,
      @NonNull OrchestrationConfiguration configuration,
      @NonNull RetryExecutor executor) {
    return command ->
        CommandOrchestrationSupport.orchestrate(
            command,
            handler.getClass(),
            handler::verify,
            nullReturningInvocation(handler),
            true,
            configuration,
            executor);
  }

  static <C extends Command>
      CommandOrchestrationSupport.CommandInvocation<C, Object> nullReturningInvocation(
          CommandHandler<C> handler) {
    return message -> {
      handler.handle(message);
      return null;
    };
  }

  static <C extends Command, R> RespondingCommandProcessor<C, R> create(
      @NonNull RespondingCommandHandler<C, R> handler) {
    return create(handler, OrchestrationConfiguration.defaults(), RetryPolicy.noRetry());
  }

  static <C extends Command, R> RespondingCommandProcessor<C, R> create(
      @NonNull RespondingCommandHandler<C, R> handler,
      @NonNull OrchestrationConfiguration configuration,
      @NonNull RetryExecutor executor) {
    return command ->
        CommandOrchestrationSupport.orchestrate(
            command,
            handler.getClass(),
            handler::verify,
            handler::handle,
            false,
            configuration,
            executor);
  }

  static <C extends Command> TokenCommandProcessor<C> create(
      @NonNull TokenCommandHandler<C> handler) {
    return create(handler, OrchestrationConfiguration.defaults(), RetryPolicy.noRetry());
  }

  static <C extends Command> TokenCommandProcessor<C> create(
      @NonNull TokenCommandHandler<C> handler,
      @NonNull OrchestrationConfiguration configuration,
      @NonNull RetryExecutor executor) {
    return command ->
        CommandOrchestrationSupport.orchestrate(
            command,
            handler.getClass(),
            handler::verify,
            handler::handle,
            false,
            configuration,
            executor);
  }
}
