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
package eu.prismacapacity.cqs.core.validator.jakarta;

import eu.prismacapacity.cqs.core.Message;
import eu.prismacapacity.cqs.core.validator.MessageValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.NonNull;

public class JakartaMessageValidator implements MessageValidator<Message> {

  private final Validator validator = fatchValidator();

  // would throw if no impl available
  @NonNull
  private Validator fatchValidator() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }

  @Override
  public void validate(@NonNull Message message) throws Throwable {
    Set<ConstraintViolation<Message>> violations = validator.validate(message);
    if (!violations.isEmpty()) {
      throw new JakartaMessageValidationException(violations);
    }
  }
}
