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

import eu.prismacapacity.cqs.aop.RetryConfiguration;
import eu.prismacapacity.cqs.core.cmd.CommandHandler;
import eu.prismacapacity.cqs.core.cmd.CommandValidationException;
import eu.prismacapacity.cqs.core.cmd.CommandVerificationException;
import eu.prismacapacity.cqs.core.query.QueryValidationException;
import java.util.NoSuchElementException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Sends a welcome email through the demo gateway after checking the account exists. */
@Component
@RequiredArgsConstructor
@RetryConfiguration(
    notRetryOn = {
      CommandValidationException.class,
      QueryValidationException.class,
      CommandVerificationException.class // just an example, by default this is retried
    })
public class SendWelcomeEmailHandler implements CommandHandler<SendWelcomeEmail> {
  @NonNull private final AccountRepository accounts;
  @NonNull private final EmailGateway emailGateway;

  @Override
  public void verify(@NonNull SendWelcomeEmail command) {
    emailGateway.recordVerification();
    if (accounts.find(command.accountId()).isEmpty()) {
      throw new NoSuchElementException("Account does not exist");
    }
  }

  @Override
  public void handle(@NonNull SendWelcomeEmail command) {
    emailGateway.send(command.accountId());
  }
}
