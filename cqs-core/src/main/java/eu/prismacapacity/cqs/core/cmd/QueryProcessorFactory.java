package eu.prismacapacity.cqs.core.cmd;

import eu.prismacapacity.cqs.core.query.*;
import lombok.NonNull;

public interface QueryProcessorFactory {

  @NonNull
  static <Q extends Query, T> QueryProcessor<Q,T> create(
      QueryHandler<Q,T> handler) {
    return q->QueryOrchestrationSupport.orchestrate(q, Query::validate, handler::verify, handler);
  }
}
