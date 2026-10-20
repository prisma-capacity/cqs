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

import eu.prismacapacity.cqs.core.query.QueryHandler;
import java.util.NoSuchElementException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Business logic only; verification is called by the core query pipeline. */
@Component
@RequiredArgsConstructor
public class FindAccountHandler implements QueryHandler<FindAccount, Account> {
  @NonNull private final AccountRepository accounts;

  @Override
  public void verify(@NonNull FindAccount query) {
    if (accounts.find(query.id()).isEmpty()) {
      throw new NoSuchElementException("Account does not exist");
    }
  }

  @NonNull
  @Override
  public Account handle(@NonNull FindAccount query) {
    return accounts.find(query.id()).orElseThrow();
  }
}
