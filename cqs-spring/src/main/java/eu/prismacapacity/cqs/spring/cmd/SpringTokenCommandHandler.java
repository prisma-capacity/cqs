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
import eu.prismacapacity.cqs.core.cmd.CommandHandlingException;
import eu.prismacapacity.cqs.core.cmd.CommandTokenResponse;
import eu.prismacapacity.cqs.core.cmd.TokenCommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

public abstract class SpringTokenCommandHandler<C extends Command>
    implements TokenCommandHandler<C> {

  private CommandExecutor commandExecutor;

  @Autowired
  public final void setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  @NonNull
  @Override
  public final CommandTokenResponse handle(@NonNull C cmd) throws CommandHandlingException {
    return commandExecutor()
        .execute(cmd, getClass(), this::validate, this::verify, this::doHandle, false);
  }

  protected abstract CommandTokenResponse doHandle(C cmd) throws CommandHandlingException;

  protected final CommandExecutor commandExecutor() {
    if (commandExecutor == null) {
      throw new IllegalStateException("CommandExecutor has not been injected");
    }
    return commandExecutor;
  }
}
