package eu.prismacapacity.cqs.core.cmd;

import lombok.NonNull;

public interface CommandProcessorFactory {

  @NonNull
  static <C extends Command, H extends ICommandHandler<C>, R> CommandProcessor<C> create(
      CommandHandler<C> h) {
    CommandOrchestrationSupport.CommandInvocation<C, R> invocation =
        CommandOrchestrationSupport.createInvocation(h);
    return c ->
        CommandOrchestrationSupport.orchestrate(
            c, Command::validate, h::verify, invocation, allowsNull(h));
  }

  @NonNull
  static <C extends Command, R> RespondingCommandProcessor<C,R> create(
      RespondingCommandHandler<C, R> h) {
    CommandOrchestrationSupport.CommandInvocation<C, CommandValueResponse<R>> invocation =
        CommandOrchestrationSupport.createInvocation(h);

    return command -> CommandOrchestrationSupport.orchestrate(
            command, Command::validate, h::verify, invocation, allowsNull(h));

  }

  @NonNull
  static <C extends Command, H extends ICommandHandler<C>, R> TokenCommandProcessor<C> create(
      TokenCommandHandler<C> h) {
    CommandOrchestrationSupport.CommandInvocation<C, CommandTokenResponse> invocation =
        CommandOrchestrationSupport.createInvocation(h);
    return c ->
        CommandOrchestrationSupport.orchestrate(
            c, Command::validate, h::verify, invocation, allowsNull(h));
  }

  static <H extends ICommandHandler<? extends Command>> boolean allowsNull(H h) {
    return h instanceof CommandHandler;
  }
}
