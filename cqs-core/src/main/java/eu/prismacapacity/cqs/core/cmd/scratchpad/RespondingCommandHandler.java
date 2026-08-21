package eu.prismacapacity.cqs.core.cmd.scratchpad;

import eu.prismacapacity.cqs.core.cmd.*;

public abstract class RespondingCommandHandler<C extends Command, T> extends AbstractCommandHandler<C>{
    abstract CommandValueResponse<T> handle(Command cmd) throws CommandHandlingException;
}
