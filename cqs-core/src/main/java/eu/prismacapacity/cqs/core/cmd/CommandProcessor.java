package eu.prismacapacity.cqs.core.cmd;

public interface  CommandProcessor<C> {

 void process(C command);
}
