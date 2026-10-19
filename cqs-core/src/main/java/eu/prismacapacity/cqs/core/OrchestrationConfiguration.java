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
package eu.prismacapacity.cqs.core;

import eu.prismacapacity.cqs.core.metrics.CqsMetrics;
import eu.prismacapacity.cqs.core.metrics.CqsMetricsSupport;
import eu.prismacapacity.cqs.core.validator.MessageValidator;
import eu.prismacapacity.cqs.core.validator.MessageValidatorFactory;
import java.util.List;
import lombok.NonNull;

/** Application-wide collaborators. Validators and metrics must support concurrent invocations. */
public record OrchestrationConfiguration(
    @NonNull List<MessageValidator<Message>> validators, @NonNull CqsMetrics metrics) {
  public OrchestrationConfiguration {
    validators = List.copyOf(validators);
  }

  /** Discovers validators and snapshots the configured metrics once when wiring processors. */
  public static OrchestrationConfiguration defaults() {
    return new OrchestrationConfiguration(
        MessageValidatorFactory.discoverValidators(), CqsMetricsSupport.getCqsMetrics());
  }
}
