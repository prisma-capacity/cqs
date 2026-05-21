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
package eu.prismacapacity.cqs.spring.cmd;

import eu.prismacapacity.cqs.core.cmd.Command;
import eu.prismacapacity.cqs.core.cmd.CommandHandler;
import eu.prismacapacity.cqs.core.cmd.CommandHandlingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

public abstract class SpringCommandHandler<C extends Command> implements CommandHandler<C> {

  private CommandExecutor commandExecutor;

  @Autowired
  public final void setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  @Override
  public final void handle(@NonNull C cmd) throws CommandHandlingException {
    commandExecutor()
        .execute(
            cmd,
            getClass(),
            this::validate,
            this::verify,
            command -> {
              doHandle(command);
              return null;
            },
            true);
  }

  protected abstract void doHandle(C cmd) throws CommandHandlingException;

  protected final CommandExecutor commandExecutor() {
    if (commandExecutor == null) {
      throw new IllegalStateException("CommandExecutor has not been injected");
    }
    return commandExecutor;
  }
}
