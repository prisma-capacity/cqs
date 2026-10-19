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

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

/** Single-threaded demo stub: transient failures happen before any delivery side effect. */
@Component
@Accessors(fluent = true)
public class EmailGateway {
  @Getter private int verifications;
  private int failuresRemaining;
  @Getter private int attempts;
  @Getter private int deliveries;

  public void failNextAttempts(int count) {
    if (count < 0) {
      throw new IllegalArgumentException("Failure count must not be negative");
    }
    failuresRemaining = count;
  }

  void recordVerification() {
    verifications++;
  }

  public void send(long accountId) {
    attempts++;
    if (failuresRemaining > 0) {
      failuresRemaining--;
      throw new EmailServiceUnavailableException();
    }
    deliveries++;
  }
}
