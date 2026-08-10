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

import eu.prismacapacity.cqs.core.validator.MessageValidatorProvider;
import java.util.Optional;
import lombok.NonNull;

public class JakartaMessageValidatorProvider
    implements MessageValidatorProvider<JakartaMessageValidator> {

  @NonNull
  @Override
  public Optional<JakartaMessageValidator> get() {
    try {
      Class.forName("jakarta.validation.Validator");
      return Optional.of(new JakartaMessageValidator());
    } catch (ClassNotFoundException ignored) {
    }
    return Optional.empty();
  }
}
