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
package eu.prismacapacity.cqs.spring.config;

import eu.prismacapacity.cqs.core.metrics.CommandMetrics;
import eu.prismacapacity.cqs.core.metrics.CqsMetrics;
import eu.prismacapacity.cqs.core.metrics.QueryMetrics;
import eu.prismacapacity.cqs.spring.cmd.CommandExecutor;
import eu.prismacapacity.cqs.spring.cmd.DefaultCommandExecutor;
import eu.prismacapacity.cqs.spring.query.DefaultQueryExecutor;
import eu.prismacapacity.cqs.spring.query.QueryExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import javax.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CqsSpringConfiguration {

  @Bean(name = "cqsCommandOrchestrationMode")
  public String cqsCommandOrchestrationMode() {
    return "executor";
  }

  @Bean
  @ConditionalOnMissingBean
  public CommandExecutor commandExecutor(Validator validator, CommandMetrics metrics) {
    return new DefaultCommandExecutor(validator, metrics);
  }

  @Bean
  @ConditionalOnMissingBean
  public QueryExecutor queryExecutor(Validator validator, QueryMetrics metrics) {
    return new DefaultQueryExecutor(validator, metrics);
  }

  @Bean
  @ConditionalOnMissingBean
  public CqsMetrics metrics(
      MeterRegistry meterRegistry,
      @Value("${cqs.command.timer-name:commandHandler.timed}") String commandHandlerTimerName,
      @Value("${cqs.query.timer-name:queryHandler.timed}") String queryHandlerTimerName,
      @Value("${cqs.query.timeout-name:queryHandler.timeOutDuringExecution}")
          String timeoutDuringQueryCounterName) {
    return new CqsMetrics(
        meterRegistry,
        queryHandlerTimerName,
        timeoutDuringQueryCounterName,
        commandHandlerTimerName);
  }
}
