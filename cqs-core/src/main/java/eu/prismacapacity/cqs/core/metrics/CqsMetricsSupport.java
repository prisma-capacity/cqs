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
package eu.prismacapacity.cqs.core.metrics;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Discovers application-provided metrics once, falling back to no-op metrics when none exist. */
@UtilityClass
public class CqsMetricsSupport {
  private static final AtomicReference<CqsMetrics> cqsMetrics =
      new AtomicReference<>(loadMetrics());

  @NonNull
  public CqsMetrics getCqsMetrics() {
    return cqsMetrics.get();
  }

  /** Overrides discovery for subsequently created default orchestration configurations. */
  public void setCqsMetrics(@NonNull CqsMetrics metrics) {
    cqsMetrics.set(metrics);
  }

  // The application supplies the META-INF/services entry for CqsMetrics.
  @NonNull
  private CqsMetrics loadMetrics() {
    return ServiceLoader.load(CqsMetrics.class).findFirst().orElseGet(NoCqsMetrics::new);
  }
}
