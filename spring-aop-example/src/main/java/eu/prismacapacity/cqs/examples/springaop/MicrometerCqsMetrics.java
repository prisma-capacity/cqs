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
package eu.prismacapacity.cqs.examples.springaop;

import eu.prismacapacity.cqs.core.metrics.CqsMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Records logical invocations, including failures and all retry attempts, in one shared registry.
 */
@Component
@RequiredArgsConstructor
public class MicrometerCqsMetrics implements CqsMetrics {
  @NonNull private final MeterRegistry meterRegistry;

  @Override
  public <T> T timedCommand(
      @NonNull String commandHandlerClass, int retryCount, @NonNull Supplier<T> fn) {
    return meterRegistry.timer("cqs.command.duration", "handler", commandHandlerClass).record(fn);
  }

  @Override
  public <T> T timedQuery(
      @NonNull String queryHandlerClass, int retryCount, @NonNull Supplier<T> fn) {
    return meterRegistry.timer("cqs.query.duration", "handler", queryHandlerClass).record(fn);
  }

  @Override
  public void logTimeout() {
    meterRegistry.counter("cqs.query.timeouts").increment();
  }
}
