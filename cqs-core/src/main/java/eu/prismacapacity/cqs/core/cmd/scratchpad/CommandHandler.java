package eu.prismacapacity.cqs.core.cmd.scratchpad;

import eu.prismacapacity.cqs.core.cmd.*;

public abstract class CommandHandler extends AbstractCommandHandler{
    abstract void handle(Command cmd) throws CommandHandlingException;
}
