package com.cjmckenzie.esjoin.model;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinSide {

  private String index;
  private String field;
  private Query query;
  private List<FieldInclude> include;

  public String getAggregateType(String type) {
    if (type.equals("text")) {
      return field + ".keyword";
    }
    return field;
  }

  public List<String> getFields() {
    List<String> fields = getInclude()
        .stream()
        .map(FieldInclude::getField)
        .distinct()
        .collect(Collectors.toList());
    fields.add(getField());

    return fields;
  }

}
