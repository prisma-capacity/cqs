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

/**
 * Application-wide metrics. A classpath ServiceLoader provider must implement this interface and
 * expose a public no-argument constructor. Register its fully qualified class name in the
 * application's {@code META-INF/services/eu.prismacapacity.cqs.core.metrics.CqsMetrics} resource.
 */
public interface CqsMetrics extends CommandMetrics, QueryMetrics {}
