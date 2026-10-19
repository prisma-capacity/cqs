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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.springframework.stereotype.Component;

/** In-memory storage for this demo, shared by the command and query handlers. */
@Component
public final class AccountRepository {
  private final Map<Long, Account> accounts = new ConcurrentHashMap<>();

  public Optional<Account> find(long id) {
    return Optional.ofNullable(accounts.get(id));
  }

  public void save(@NonNull Account account) {
    if (accounts.putIfAbsent(account.id(), account) != null) {
      throw new IllegalStateException("Account already exists");
    }
  }
}
