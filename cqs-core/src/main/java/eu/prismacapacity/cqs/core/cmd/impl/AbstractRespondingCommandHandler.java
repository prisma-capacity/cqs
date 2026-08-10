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
package eu.prismacapacity.cqs.core.cmd.impl;

import eu.prismacapacity.cqs.core.cmd.*;
import eu.prismacapacity.cqs.core.metrics.CqsMetrics;
import eu.prismacapacity.cqs.core.metrics.CqsMetricsSupport;
import eu.prismacapacity.cqs.core.retry.RetryExecutor;
import eu.prismacapacity.cqs.core.retry.RetrySupport;
import lombok.NonNull;

public abstract class AbstractRespondingCommandHandler<C extends Command, T>
    implements RespondingCommandHandler<C, T> {

  public final RetryExecutor retryExecutor = RetrySupport.getExecutor();
  public final CqsMetrics cqsMetrics = CqsMetricsSupport.getCqsMetrics();

  @NonNull
  @Override
  public final CommandValueResponse<T> handle(@NonNull C cmd) throws CommandHandlingException {
    return retryExecutor.execute(
        this.getClass(),
        retryCount ->
            cqsMetrics.timedCommand(this.getClass().getName(), retryCount, () -> process(cmd)));
  }

  private CommandValueResponse<T> process(@NonNull C cmd) {
    return CommandOrchestrationSupport.orchestrate(
        cmd, this::validate, this::verify, this::doHandle, false);
  }

  @Override
  public abstract void verify(@NonNull C cmd) throws CommandVerificationException;

  abstract CommandValueResponse<T> doHandle(@NonNull C cmd) throws CommandHandlingException;
}
