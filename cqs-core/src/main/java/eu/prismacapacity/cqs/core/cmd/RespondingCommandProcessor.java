package eu.prismacapacity.cqs.core.cmd;

public interface RespondingCommandProcessor<C, R> {
  CommandValueResponse<R> process(C command);
}
