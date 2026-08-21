package eu.prismacapacity.cqs.core.cmd;

public interface TokenCommandProcessor<C> {
  CommandTokenResponse process(C command);
}
