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

class MigrateHandlersHierarchyRecipeTest implements RewriteTest {

  Parser.Builder parser = JavaParser.fromJavaVersion().classpath("cqs-core", "cqs-spring");

  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipe(new MigrateHandlersHierarchyRecipe()).parser(parser);
  }

  @Test
  void replacesImplementsWithExtends() {
    rewriteRun(
        java(
            """
              package eu.prismacapacity;

              import eu.prismacapacity.cqs.core.cmd.Command;
              import eu.prismacapacity.cqs.spring.cmd.SpringRespondingCommandHandler;

              class FooBar implements SpringRespondingCommandHandler<Command, String>, Runnable {
              }
              """,
            """
              package eu.prismacapacity;

              import eu.prismacapacity.cqs.core.cmd.Command;
              import eu.prismacapacity.cqs.spring.cmd.SpringRespondingCommandHandler;

              class FooBar extends SpringRespondingCommandHandler<Command, String> implements Runnable {
              }
              """));
  }
}
