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

import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateHandlersHierarchyRecipe extends Recipe {
  String displayName = "Migrate Handler from implements to extends";
  String description =
      "Converts Handler interfaces implemented by classes into inheritance from their corresponding abstract base classes by moving matching types from the implements clause to the extends clause without altering method signatures or type references.";

  @Override
  public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
    return new JavaIsoVisitor<>() {
      final Pattern springCommandHandler =
          Pattern.compile("^eu\\.prismacapacity\\.cqs\\.spring\\.cmd\\..*CommandHandler$");
      final Pattern springQueryHandler =
          Pattern.compile("^eu\\.prismacapacity\\.cqs\\.spring\\.query\\..*QueryHandler$");

      @Override
      public J.@NonNull ClassDeclaration visitClassDeclaration(
          J.@NonNull ClassDeclaration classDecl, @NonNull ExecutionContext ctx) {
        final var cd = super.visitClassDeclaration(classDecl, ctx);

        final var typeImplements = cd.getImplements();
        if (typeImplements == null || typeImplements.isEmpty()) {
          return cd;
        }

        final var optTarget =
            cd.getImplements().stream()
                .filter(
                    impl -> {
                      final var fq = TypeUtils.asFullyQualified(impl.getType());
                      return fq != null
                          && (springCommandHandler.matcher(fq.getFullyQualifiedName()).matches()
                              || springQueryHandler.matcher(fq.getFullyQualifiedName()).matches());
                    })
                .findFirst();

        return optTarget
            .map(
                target -> {
                  final var newCd =
                      cd.withImplements(
                              typeImplements.stream().filter(impl -> !impl.equals(target)).toList())
                          .withExtends(target);
                  return autoFormat(newCd, ctx);
                })
            .orElse(cd);
      }
    };
  }
}
