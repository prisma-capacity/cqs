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
package eu.prismacapacity.cqs.core.query;

import java.util.Set;
import java.util.concurrent.TimeoutException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QueryOrchestrationSupport {

  public <Q extends Query, T> T orchestrate(
      @NonNull Q query,
      @NonNull Validator validator,
      @NonNull QueryStep<Q> validateStep,
      @NonNull QueryStep<Q> verifyStep,
      @NonNull QueryInvocation<Q, T> handleStep,
      @NonNull Runnable timeoutHook) {
    Set<ConstraintViolation<Q>> violations = validator.validate(query);
    if (!violations.isEmpty()) {
      throw new QueryValidationException(violations);
    }

    try {
      validateStep.perform(query);
    } catch (QueryValidationException e) {
      throw e;
    } catch (Throwable e) {
      throw new QueryValidationException(e);
    }

    try {
      verifyStep.perform(query);
    } catch (QueryVerificationException e) {
      throw e;
    } catch (Throwable e) {
      throw new QueryVerificationException(e);
    }

    T result;
    try {
      result = handleStep.invoke(query);
    } catch (TimeoutException e) {
      timeoutHook.run();
      throw new QueryTimeoutException(e);
    } catch (QueryTimeoutException e) {
      timeoutHook.run();
      throw e;
    } catch (QueryHandlingException e) {
      throw e;
    } catch (Throwable e) {
      throw new QueryHandlingException(e);
    }

    if (result == null) {
      throw new QueryHandlingException("Returned object must not be null");
    }

    return result;
  }

  @FunctionalInterface
  public interface QueryStep<Q extends Query> {
    void perform(@NonNull Q query) throws Throwable;
  }

  @FunctionalInterface
  public interface QueryInvocation<Q extends Query, T> {
    T invoke(@NonNull Q query) throws Throwable;
  }
}
