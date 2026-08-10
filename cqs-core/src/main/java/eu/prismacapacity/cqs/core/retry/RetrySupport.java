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
package eu.prismacapacity.cqs.core.retry;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RetrySupport {
  private static final AtomicReference<RetryExecutor> executor =
      new AtomicReference<>(loadExecutor());

  @NonNull
  public RetryExecutor getExecutor() {
    return executor.get();
  }

  // might be useful for fw impl without service loader
  public void setExecutor(@NonNull RetryExecutor customExecutor) {
    executor.set(customExecutor);
  }

  // allows using service loader to get the RetryExecutor (like the validator approach)
  @NonNull
  private RetryExecutor loadExecutor() {
    return ServiceLoader.load(RetryExecutor.class).findFirst().orElseGet(NoRetryExecutor::new);
  }
}
