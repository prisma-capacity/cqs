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

import eu.prismacapacity.cqs.core.cmd.Command;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/** Welcome-email request; the validation count makes the demo's retry boundary observable. */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class SendWelcomeEmail implements Command {
  private final long accountId;
  private int validations;

  @Override
  public void validate() {
    validations++;
    if (accountId <= 0) {
      throw new IllegalArgumentException("A positive account id is required");
    }
  }
}
