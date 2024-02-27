package com.cjmckenzie.esjoin.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MappingType {
  private static final String ALIAS_TYPE = "alias";

  private String type;
  private String aliasPath;

  public boolean isAliasType() {
    return ALIAS_TYPE.equals(type);
  }
}
