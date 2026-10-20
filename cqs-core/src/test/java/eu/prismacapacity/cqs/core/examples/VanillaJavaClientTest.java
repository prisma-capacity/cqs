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
package eu.prismacapacity.cqs.core.examples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import eu.prismacapacity.cqs.core.OrchestrationConfiguration;
import eu.prismacapacity.cqs.core.cmd.*;
import eu.prismacapacity.cqs.core.query.Query;
import eu.prismacapacity.cqs.core.query.QueryHandler;
import eu.prismacapacity.cqs.core.query.QueryValidationException;
import eu.prismacapacity.cqs.core.query.QueryVerificationException;
import eu.prismacapacity.cqs.core.retry.SimpleRetryExecutor;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.junit.Test;

/** Client examples exercising real processors, messages, handlers and an in-memory repository. */
public class VanillaJavaClientTest {

  @Test
  public void createsAnAccountWithACommandAndReadsItWithAQuery() {
    var repo = new InMemoryAccounts();
    var createAccount = CommandProcessorFactory.create(new CreateAccountHandler(repo));
    var findAccount = QueryProcessorFactory.create(new FindAccountHandler(repo));

    createAccount.process(new CreateAccount(1, "Alice"));
    var account = findAccount.process(new FindAccount(1));

    assertEquals(new Account(1, "Alice"), account);
  }

  @Test
  public void rejectsAnInvalidCommandBeforeSavingAnything() {
    var repo = new InMemoryAccounts();
    var createAccount = CommandProcessorFactory.create(new CreateAccountHandler(repo));

    var failure =
        assertThrows(
            CommandValidationException.class,
            () -> createAccount.process(new CreateAccount(1, " ")));

    assertTrue(failure.getCause() instanceof IllegalArgumentException);
    assertFalse(repo.find(1).isPresent());
  }

  @Test
  public void rejectsAnInvalidQueryBeforeLookingUpAnAccount() {
    var repo = new InMemoryAccounts();
    var findAccount = QueryProcessorFactory.create(new FindAccountHandler(repo));

    var failure =
        assertThrows(QueryValidationException.class, () -> findAccount.process(new FindAccount(0)));

    assertTrue(failure.getCause() instanceof IllegalArgumentException);
  }

  @Test
  public void verifiesThatACommandCannotOverwriteAnExistingAccount() {
    var repo = new InMemoryAccounts();
    var createAccount = CommandProcessorFactory.create(new CreateAccountHandler(repo));
    var findAccount = QueryProcessorFactory.create(new FindAccountHandler(repo));
    createAccount.process(new CreateAccount(1, "Alice"));

    assertThrows(
        CommandVerificationException.class,
        () -> createAccount.process(new CreateAccount(1, "Bob")));

    assertEquals(new Account(1, "Alice"), findAccount.process(new FindAccount(1)));
  }

  @Test
  public void verifiesThatAQueriedAccountExists() {
    var repo = new InMemoryAccounts();
    var findAccount = QueryProcessorFactory.create(new FindAccountHandler(repo));

    var failure =
        assertThrows(
            QueryVerificationException.class, () -> findAccount.process(new FindAccount(42)));

    assertTrue(failure.getCause() instanceof NoSuchElementException);
  }

  @Test
  public void optsIntoCommandRetries() {
    var repo = new FailingInMemoryAccounts();
    repo.setFailNextAttempts(2);
    var createAccount = CommandProcessorWithRetryFactory.create(new CreateAccountHandler(repo));
    var findAccount = QueryProcessorFactory.create(new FindAccountHandler(repo));

    createAccount.process(new CreateAccount(1, "Alice"));

    assertEquals(3, repo.getAttempts());
    assertEquals(new Account(1, "Alice"), findAccount.process(new FindAccount(1)));
  }

  private record Account(long id, String name) {}

  private record CreateAccount(long id, String name) implements Command {
    @Override
    public void validate() {
      if (id <= 0 || name == null || name.isBlank()) {
        throw new IllegalArgumentException("A positive account id and a name are required");
      }
    }
  }

  private record FindAccount(long id) implements Query {
    @Override
    public void validate() {
      if (id <= 0) {
        throw new IllegalArgumentException("A positive account id is required");
      }
    }
  }

  private record CreateAccountHandler(@NonNull AccountRepository accounts)
      implements CommandHandler<CreateAccount> {
    @Override
    public void verify(CreateAccount command) {
      if (accounts.find(command.id()).isPresent()) {
        throw new IllegalStateException("Account already exists");
      }
    }

    @Override
    public void handle(@NonNull CreateAccount command) {
      accounts.save(new Account(command.id(), command.name()));
    }
  }

  private record FindAccountHandler(@NonNull AccountRepository accounts)
      implements QueryHandler<FindAccount, Account> {
    @Override
    public void verify(@NonNull FindAccount query) {
      if (accounts.find(query.id()).isEmpty()) {
        throw new NoSuchElementException("Account does not exist");
      }
    }

    @Override
    public @NonNull Account handle(@NonNull FindAccount query) {
      return accounts.find(query.id()).orElseThrow();
    }
  }

  private interface AccountRepository {
    Optional<Account> find(long id);

    void save(Account account);
  }

  private static class InMemoryAccounts implements AccountRepository {
    protected final Map<Long, Account> accounts = new HashMap<>();

    @Override
    public Optional<Account> find(long id) {
      return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public void save(Account account) {
      accounts.put(account.id(), account);
    }
  }

  private static class FailingInMemoryAccounts extends InMemoryAccounts {

    @Setter private int failNextAttempts;
    @Getter private int attempts;

    @Override
    public void save(Account account) {
      if (failNextAttempts - attempts++ > 0) {
        throw new TemporarilyUnavailableException();
      }
      accounts.put(account.id(), account);
    }
  }

  private static final class TemporarilyUnavailableException extends RuntimeException {}

  private static final class CommandProcessorWithRetryFactory implements CommandProcessorFactory {
    static <C extends Command> CommandProcessor<C> create(@NonNull CommandHandler<C> handler) {
      var retryExecutor =
          new SimpleRetryExecutor(5, cause -> cause instanceof TemporarilyUnavailableException);
      return command ->
          CommandOrchestrationSupport.orchestrate(
              command,
              handler.getClass(),
              handler::verify,
              CommandProcessorFactory.nullReturningInvocation(handler),
              true,
              OrchestrationConfiguration.defaults(),
              retryExecutor);
    }
  }
}
