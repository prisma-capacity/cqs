package eu.prismacapacity.cqs.core.cmd.scratchpad;

import eu.prismacapacity.cqs.core.cmd.*;

public abstract class TokenCommandHandler<C extends Command> extends AbstractCommandHandler<C> {
  abstract CommandTokenResponse handle(Command cmd) throws CommandHandlingException;
}
