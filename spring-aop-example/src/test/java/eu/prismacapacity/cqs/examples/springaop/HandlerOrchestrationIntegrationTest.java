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

import static org.junit.jupiter.api.Assertions.*;

import eu.prismacapacity.cqs.core.cmd.CommandHandlingException;
import eu.prismacapacity.cqs.core.cmd.CommandValidationException;
import eu.prismacapacity.cqs.core.cmd.CommandVerificationException;
import eu.prismacapacity.cqs.core.query.QueryValidationException;
import eu.prismacapacity.cqs.core.query.QueryVerificationException;
import eu.prismacapacity.cqs.core.retry.RetryExecutor;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.annotation.DirtiesContext;

/** Exercises the example's real Spring Boot wiring, AOP advice, handlers and repository. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class HandlerOrchestrationIntegrationTest {
  @Autowired private CreateAccountHandler createAccountHandler;
  @Autowired private FindAccountHandler findAccountHandler;
  @Autowired private AccountRepository accounts;

  @Autowired private SendWelcomeEmailHandler sendWelcomeEmailHandler;
  @Autowired private EmailGateway emailGateway;

  @Test
  public void commandCreatesAnAccount() {
    createAccountHandler.handle(new CreateAccount(1, "Alice"));

    assertEquals(new Account(1, "Alice"), accounts.find(1).orElseThrow());
  }

  @Test
  public void queryReturnsAnExistingAccount() {
    var account = new Account(1, "Alice");
    accounts.save(account);

    assertEquals(account, findAccountHandler.handle(new FindAccount(1)));
  }

  @Test
  public void invalidCommandFailsValidationWithoutSaving() {
    var failure =
        assertThrows(
            CommandValidationException.class,
            () -> createAccountHandler.handle(new CreateAccount(1, " ")));

    assertInstanceOf(CommandValidationException.class, failure);
    assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    assertFalse(accounts.find(1).isPresent());
  }

  @Test
  public void invalidQueryFailsValidationBeforeVerification() {
    // A missing account would fail verification if validation did not run first.
    var failure =
        assertThrows(
            QueryValidationException.class, () -> findAccountHandler.handle(new FindAccount(0)));

    assertInstanceOf(QueryValidationException.class, failure);
    assertInstanceOf(IllegalArgumentException.class, failure.getCause());
  }

  @Test
  public void duplicateCommandFailsVerificationWithoutChangingTheAccount() {
    var original = new Account(1, "Alice");
    accounts.save(original);

    var failure =
        assertThrows(
            CommandVerificationException.class,
            () -> createAccountHandler.handle(new CreateAccount(1, "Bob")));

    assertInstanceOf(CommandVerificationException.class, failure);
    assertInstanceOf(IllegalStateException.class, failure.getCause());
    assertEquals(original, accounts.find(1).orElseThrow());
  }

  @Test
  public void missingAccountQueryFailsVerification() {
    var failure =
        assertThrows(
            QueryVerificationException.class, () -> findAccountHandler.handle(new FindAccount(1)));

    assertInstanceOf(QueryVerificationException.class, failure);
    assertInstanceOf(NoSuchElementException.class, failure.getCause());
  }

  @Test
  public void welcomeEmailCommandSendsForAnExistingAccount() {
    createAccountHandler.handle(new CreateAccount(1, "Alice"));

    sendWelcomeEmailHandler.handle(new SendWelcomeEmail(1));

    assertEquals(1, emailGateway.attempts());
    assertEquals(1, emailGateway.deliveries());
  }

  @Test
  public void welcomeEmailTransientFailureIsRetriedOnlyForTheEmailHandler() {
    createAccountHandler.handle(new CreateAccount(1, "Alice"));
    emailGateway.failNextAttempts(1);

    var command = new SendWelcomeEmail(1);
    sendWelcomeEmailHandler.handle(command);

    assertEquals(1, command.validations());
    assertEquals(2, emailGateway.verifications());
    assertEquals(2, emailGateway.attempts());
    assertEquals(1, emailGateway.deliveries());
    // The second use case does not change existing command/query behavior.
    createAccountHandler.handle(new CreateAccount(2, "Bob"));
    assertEquals(new Account(2, "Bob"), findAccountHandler.handle(new FindAccount(2)));
  }

  @Test
  public void emailRetryExhaustionPreservesTheHandlingFailure() {
    accounts.save(new Account(1, "Alice"));
    emailGateway.failNextAttempts(5);
    var command = new SendWelcomeEmail(1);

    var failure =
        assertThrows(CommandHandlingException.class, () -> sendWelcomeEmailHandler.handle(command));

    assertInstanceOf(EmailServiceUnavailableException.class, failure.getCause());
    assertEquals(3, emailGateway.attempts());
    assertEquals(3, emailGateway.verifications());
    assertEquals(1, command.validations());
    assertEquals(0, emailGateway.deliveries());
  }

  @Test
  public void emailValidationAndVerificationFailuresAreNotRetried() {
    var invalid = new SendWelcomeEmail(0);
    assertThrows(CommandValidationException.class, () -> sendWelcomeEmailHandler.handle(invalid));
    assertEquals(1, invalid.validations());
    assertEquals(0, emailGateway.verifications());

    var missing = new SendWelcomeEmail(1);
    assertThrows(CommandVerificationException.class, () -> sendWelcomeEmailHandler.handle(missing));
    assertEquals(1, missing.validations());
    assertEquals(1, emailGateway.verifications());
    assertEquals(0, emailGateway.attempts());
  }

  @Test
  public void unconfiguredHandlerDoesNotRetryTheSameTransientFailure() {
    emailGateway.failNextAttempts(1);
    var handler = singleAttemptEmailHandler;
    assertThrows(CommandHandlingException.class, () -> handler.handle(new SendWelcomeEmail(1)));
    assertEquals(1, emailGateway.attempts());
    assertEquals(0, emailGateway.deliveries());
  }

  @Autowired private SingleAttemptEmailHandler singleAttemptEmailHandler;

  @TestConfiguration(proxyBeanMethods = false)
  static class EmailConfiguration {
    @Bean
    Map<Class<?>, RetryExecutor> handlerRetryExecutors() {
      var template =
          RetryTemplate.builder()
              .maxAttempts(3)
              .retryOn(
                  failure ->
                      failure instanceof CommandHandlingException
                          && failure.getCause() instanceof EmailServiceUnavailableException)
              .build();
      RetryExecutor executor =
          new RetryExecutor() {
            @Override
            public <T> T execute(Class<?> handler, Function<Integer, T> attempt) {
              return template.execute(context -> attempt.apply(context.getRetryCount()));
            }
          };
      return Map.of(SendWelcomeEmailHandler.class, executor);
    }
  }
}
