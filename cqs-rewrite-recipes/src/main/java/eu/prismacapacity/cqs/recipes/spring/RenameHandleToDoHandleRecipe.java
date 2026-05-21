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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class RenameHandleToDoHandleRecipe extends Recipe {
  String displayName = "Rename 'handle' method to 'doHandle' in Spring handlers";
  String description =
      "Renames 'handle' methods to 'doHandle' only in Spring handler classes and their subclasses, preserving method signatures and parameters.";

  @Override
  public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
    final Pattern springCommandHandler =
        Pattern.compile("^eu\\.prismacapacity\\.cqs\\.spring\\.cmd\\..*CommandHandler$");
    final Pattern springQueryHandler =
        Pattern.compile("^eu\\.prismacapacity\\.cqs\\.spring\\.query\\..*QueryHandler$");

    return new JavaIsoVisitor<>() {

      @Override
      public J.@NonNull MethodDeclaration visitMethodDeclaration(
          J.@NonNull MethodDeclaration method, @NonNull ExecutionContext ctx) {
        final var m = super.visitMethodDeclaration(method, ctx);
        if (!"handle".equals(m.getSimpleName())) {
          return m;
        }

        // must be inside a Spring handler hierarchy
        final var methodType = m.getMethodType();
        if (methodType == null) {
          return m;
        }

        final var owner = TypeUtils.asFullyQualified(methodType.getDeclaringType());
        if (owner == null) {
          return m;
        }
        if (!TypeUtils.isAssignableTo(springCommandHandler, owner)
            && !TypeUtils.isAssignableTo(springQueryHandler, owner)) {
          return m;
        }

        // rename method, keep signature unchanged
        final var newM = m.withName(m.getName().withSimpleName("doHandle"));
        return autoFormat(newM, ctx);
      }
    };
  }
}
