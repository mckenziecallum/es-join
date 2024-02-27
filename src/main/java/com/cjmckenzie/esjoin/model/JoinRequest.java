package com.cjmckenzie.esjoin.model;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequest {
  private JoinRequestParams join;
  private Query query;
  private int size;
  private int offset;
}
