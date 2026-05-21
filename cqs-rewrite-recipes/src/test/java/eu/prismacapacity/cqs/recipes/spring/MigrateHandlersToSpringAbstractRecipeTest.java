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
package eu.prismacapacity.cqs.recipes.spring;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

class MigrateHandlersToSpringAbstractRecipeTest implements RewriteTest {

  Parser.Builder parser =
      JavaParser.fromJavaVersion().classpath("spring-cqs", "lombok", "cqs-core", "cqs-spring");

  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipeFromResources("eu.prismacapacity.cqs.spring.MigrateHandlersToSpringAbstract")
        .parser(parser)
        .afterTypeValidationOptions(
            TypeValidation.none()); // needed to avoid issues with clashing types
  }

  @Test
  void replaces() {
    rewriteRun(
        java(
            """
              package eu.prismacapacity;

              import eu.prismacapacity.spring.cqs.cmd.Command;
              import eu.prismacapacity.spring.cqs.cmd.CommandValueResponse;
              import eu.prismacapacity.spring.cqs.cmd.RespondingCommandHandler;
              import lombok.NonNull;

              class FooBar implements RespondingCommandHandler<Command, String>, Runnable {
                  @NonNull
                  @Override
                  public CommandValueResponse<String> handle(@NonNull Command cmd) {
                      return CommandValueResponse.empty();
                  }

                  @Override
                  public void verify(@NonNull Command cmd) {
                  }

                  @Override
                  public void run() {
                  }
              }
              """,
            """
              package eu.prismacapacity;

              import eu.prismacapacity.cqs.core.cmd.Command;
              import eu.prismacapacity.cqs.core.cmd.CommandValueResponse;
              import eu.prismacapacity.cqs.spring.cmd.SpringRespondingCommandHandler;
              import lombok.NonNull;

              class FooBar extends SpringRespondingCommandHandler<Command, String> implements Runnable {
                  @NonNull
                  @Override
                  public CommandValueResponse<String> doHandle(@NonNull Command cmd) {
                      return CommandValueResponse.empty();
                  }

                  @Override
                  public void verify(@NonNull Command cmd) {
                  }

                  @Override
                  public void run() {
                  }
              }
              """));
  }
}
