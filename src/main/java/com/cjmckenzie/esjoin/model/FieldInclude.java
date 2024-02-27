package com.cjmckenzie.esjoin.model;

import lombok.Data;

import java.util.Optional;

@Data
public class FieldInclude {
  private String field;
  private String alias;

  public String getOutputField() {
    return Optional.ofNullable(alias).orElse(field);
  }
}
