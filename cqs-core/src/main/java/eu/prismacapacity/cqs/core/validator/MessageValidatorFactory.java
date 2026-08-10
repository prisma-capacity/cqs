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
package eu.prismacapacity.cqs.core.validator;

import eu.prismacapacity.cqs.core.Message;
import java.util.*;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageValidatorFactory {
  @NonNull
  public List<MessageValidator<Message>> discoverValidators() {
    return providers().stream()
        .map(MessageValidatorProvider::get)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  @NonNull
  private List<MessageValidatorProvider<MessageValidator<Message>>> providers() {
    final var services = new ArrayList<MessageValidatorProvider<MessageValidator<Message>>>();
    final var loader = ServiceLoader.load(MessageValidatorProvider.class);
    loader.forEach(services::add);
    return services;
  }
}
