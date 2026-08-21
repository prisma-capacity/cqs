package eu.prismacapacity.cqs.core.cmd;

public interface QueryProcessor<Q, T> {
    T process(Q query);
}
