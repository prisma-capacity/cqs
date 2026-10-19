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

import eu.prismacapacity.cqs.core.OrchestrationConfiguration;
import eu.prismacapacity.cqs.core.metrics.CqsMetrics;
import eu.prismacapacity.cqs.core.retry.RetryExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ExampleConfiguration {
  @Bean
  public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }

  @Bean
  public OrchestrationConfiguration orchestrationConfiguration(@NonNull CqsMetrics metrics) {
    // All handlers share metrics; message validation remains mandatory.
    return new OrchestrationConfiguration(List.of(), metrics);
  }

  @Bean
  public HandlerOrchestrationAspect handlerOrchestrationAspect(
      OrchestrationConfiguration configuration,
      @Qualifier("handlerRetryExecutors") ObjectProvider<Map<Class<?>, RetryExecutor>> executors) {
    // Only handlers requiring retries need an entry keyed by their implementation class.
    // Metrics and validators are shared application-wide; other handlers default to no retry.
    return new HandlerOrchestrationAspect(configuration, executors.getIfAvailable(Map::of));
  }
}
