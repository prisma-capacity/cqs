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
package eu.prismacapacity.spring.cqs.spring.query;

import eu.prismacapacity.spring.cqs.core.query.Query;
import eu.prismacapacity.spring.cqs.core.query.QueryHandler;
import eu.prismacapacity.spring.cqs.core.query.QueryHandlingException;
import eu.prismacapacity.spring.cqs.core.query.QueryTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

public abstract class SpringQueryHandler<Q extends Query, T> implements QueryHandler<Q, T> {

  private QueryExecutor queryExecutor;

  @Autowired
  public final void setQueryExecutor(QueryExecutor queryExecutor) {
    this.queryExecutor = queryExecutor;
  }

  @NonNull
  @Override
  public final T handle(@NonNull Q query) throws QueryHandlingException, QueryTimeoutException {
    return queryExecutor().execute(query, getClass(), this::validate, this::verify, this::doHandle);
  }

  protected abstract T doHandle(Q query) throws QueryHandlingException, QueryTimeoutException;

  protected final QueryExecutor queryExecutor() {
    if (queryExecutor == null) {
      throw new IllegalStateException("QueryExecutor has not been injected");
    }
    return queryExecutor;
  }
}
