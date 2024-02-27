package com.cjmckenzie.esjoin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestParams {
  private JoinSide left;
  private JoinSide right;
  private JoinType type;
}
